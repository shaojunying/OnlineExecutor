package io.github.shaojunying.onlineexecutor.runner.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.shaojunying.onlineexecutor.compile.CompiledProgram;
import io.github.shaojunying.onlineexecutor.runner.BoundedOutput;
import io.github.shaojunying.onlineexecutor.runner.CodeRunner;
import io.github.shaojunying.onlineexecutor.runner.RunResult;
import io.github.shaojunying.onlineexecutor.runner.RunStatus;

/**
 * Runs user code in a child JVM.
 *
 * <p>The class files go to a temporary directory. The child JVM gets a heap limit and an
 * empty environment, so it cannot read server secrets from environment variables.
 * On timeout or when the output is too large, the runner kills the process and all its
 * child processes.
 *
 * <p>This mode protects the server from crashes, {@code System.exit}, endless loops and
 * memory exhaustion. It does not stop user code from reading files or using the network.
 * {@link DockerRunner} adds that isolation.
 */
public class ProcessRunner implements CodeRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessRunner.class);
    private static final Duration DRAIN_TIMEOUT = Duration.ofSeconds(2);
    private static final boolean POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

    protected final Duration timeout;
    protected final int maxOutputBytes;
    protected final int heapMb;

    public ProcessRunner(Duration timeout, int maxOutputBytes, int heapMb) {
        this.timeout = timeout;
        this.maxOutputBytes = maxOutputBytes;
        this.heapMb = heapMb;
    }

    @Override
    public RunResult run(CompiledProgram program, String stdin) {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        Path classDir = null;
        try {
            classDir = writeClasses(program.classes());
            ProcessBuilder builder = new ProcessBuilder(command(classDir, program.mainClassName(), runId))
                    .directory(classDir.toFile());
            prepareEnvironment(builder.environment());
            return execute(builder, stdin, runId);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start the user program", e);
        } finally {
            deleteQuietly(classDir);
        }
    }

    /** Command that starts the child JVM. */
    protected List<String> command(Path classDir, String mainClass, String runId) {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(jvmOptions());
        command.addAll(List.of("-cp", classDir.toString(), mainClass));
        return command;
    }

    /** JVM options that keep the child small and start it fast. */
    protected List<String> jvmOptions() {
        return List.of(
                "-Xmx" + heapMb + "m",
                "-XX:+UseSerialGC",
                "-XX:TieredStopAtLevel=1",
                "-XX:-UsePerfData",
                "-Dfile.encoding=UTF-8",
                "-Dstdout.encoding=UTF-8",
                "-Dstderr.encoding=UTF-8");
    }

    /** Removes all environment variables except the ones the JVM needs to start. */
    protected void prepareEnvironment(Map<String, String> environment) {
        String systemRoot = environment.get("SystemRoot");
        environment.clear();
        if (systemRoot != null) {
            environment.put("SystemRoot", systemRoot);
        }
    }

    /** Stops the user program. Subclasses add cleanup for their own runtime. */
    protected void kill(Process process, String runId) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private RunResult execute(ProcessBuilder builder, String stdin, String runId) throws IOException {
        BoundedOutput stdout = new BoundedOutput(maxOutputBytes);
        BoundedOutput stderr = new BoundedOutput(maxOutputBytes);
        AtomicBoolean overflowed = new AtomicBoolean();

        long start = System.nanoTime();
        Process process = builder.start();
        Runnable onOverflow = () -> {
            if (overflowed.compareAndSet(false, true)) {
                kill(process, runId);
            }
        };
        Thread outPump = pump(process.getInputStream(), stdout, onOverflow);
        Thread errPump = pump(process.getErrorStream(), stderr, onOverflow);
        feed(process.getOutputStream(), stdin);

        boolean finished = waitFor(process, timeout);
        if (!finished) {
            kill(process, runId);
            waitFor(process, DRAIN_TIMEOUT);
        }
        joinQuietly(outPump);
        joinQuietly(errPump);
        long durationMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

        if (overflowed.get()) {
            return new RunResult(RunStatus.OUTPUT_LIMIT_EXCEEDED, stdout.content(), stderr.content(), null, durationMillis);
        }
        if (!finished) {
            return new RunResult(RunStatus.TIMEOUT, stdout.content(), stderr.content(), null, durationMillis);
        }
        return RunResult.fromExitCode(process.exitValue(), stdout.content(), stderr.content(), durationMillis);
    }

    private static Thread pump(InputStream source, BoundedOutput target, Runnable onOverflow) {
        return Thread.ofVirtual().start(() -> {
            byte[] chunk = new byte[8192];
            try (source) {
                int n;
                while ((n = source.read(chunk)) != -1) {
                    if (!target.write(chunk, 0, n)) {
                        onOverflow.run();
                    }
                }
            } catch (IOException e) {
                // The process ended or was killed.
            }
        });
    }

    private static void feed(OutputStream stdinOfChild, String stdin) {
        Thread.ofVirtual().start(() -> {
            try (stdinOfChild) {
                stdinOfChild.write(stdin.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                // The program ended without reading all input.
            }
        });
    }

    private static boolean waitFor(Process process, Duration duration) {
        try {
            return process.waitFor(duration.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void joinQuietly(Thread thread) {
        try {
            thread.join(DRAIN_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Path writeClasses(Map<String, byte[]> classes) throws IOException {
        Path root = Files.createTempDirectory("online-executor-");
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            Path file = root.resolve(entry.getKey().replace('.', '/') + ".class");
            Files.createDirectories(file.getParent());
            Files.write(file, entry.getValue());
        }
        if (POSIX) {
            // The docker container runs as an unprivileged user and must be able to read the files.
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.toList()) {
                    String mode = Files.isDirectory(path) ? "rwxr-xr-x" : "rw-r--r--";
                    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode));
                }
            }
        }
        return root;
    }

    private static void deleteQuietly(Path root) {
        if (root == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            log.warn("Could not delete {}", root, e);
        }
    }
}
