package io.github.shaojunying.onlineexecutor.runner.process;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.shaojunying.onlineexecutor.config.ExecutorProperties;

/**
 * Runs user code in a child JVM inside a locked-down Docker container.
 *
 * <p>The container has no network, a read-only file system, a memory, CPU and process
 * limit, no Linux capabilities, and runs as the unprivileged user {@code nobody}.
 * The class files are mounted read-only.
 *
 * <p>When the server itself runs in a container, the temporary directory must be a
 * path that the Docker daemon can see. See the README.
 */
public class DockerRunner extends ProcessRunner {

    private static final Logger log = LoggerFactory.getLogger(DockerRunner.class);
    private static final String CODE_DIR = "/code";

    private final ExecutorProperties.Docker docker;

    public DockerRunner(Duration timeout, int maxOutputBytes, int heapMb, ExecutorProperties.Docker docker) {
        super(timeout, maxOutputBytes, heapMb);
        this.docker = docker;
    }

    @Override
    protected List<String> command(Path classDir, String mainClass, String runId) {
        List<String> command = new ArrayList<>(List.of(
                docker.executable(), "run", "--rm", "--interactive",
                "--name", containerName(runId),
                "--network", "none",
                "--memory", docker.memoryMb() + "m",
                "--memory-swap", docker.memoryMb() + "m",
                "--cpus", docker.cpus(),
                "--pids-limit", String.valueOf(docker.pidsLimit()),
                "--read-only",
                "--tmpfs", "/tmp:rw,size=16m",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",
                "--user", "65534:65534",
                "--mount", "type=bind,source=" + classDir.toAbsolutePath() + ",target=" + CODE_DIR + ",readonly",
                "--workdir", CODE_DIR,
                docker.image(),
                "java"));
        command.addAll(jvmOptions());
        command.addAll(List.of("-cp", CODE_DIR, mainClass));
        return command;
    }

    /** The docker CLI needs the user environment to find the daemon. The container gets no variables. */
    @Override
    protected void prepareEnvironment(Map<String, String> environment) {
    }

    /** Killing the docker CLI does not stop the container, so stop it by name. */
    @Override
    protected void kill(Process process, String runId) {
        try {
            Process killer = new ProcessBuilder(docker.executable(), "kill", containerName(runId))
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!killer.waitFor(10, TimeUnit.SECONDS)) {
                killer.destroyForcibly();
            }
        } catch (IOException e) {
            log.warn("Could not kill container {}", containerName(runId), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        super.kill(process, runId);
    }

    private static String containerName(String runId) {
        return "online-executor-" + runId;
    }
}
