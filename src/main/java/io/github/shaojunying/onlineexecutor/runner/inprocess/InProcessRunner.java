package io.github.shaojunying.onlineexecutor.runner.inprocess;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.github.shaojunying.onlineexecutor.compile.CompiledProgram;
import io.github.shaojunying.onlineexecutor.runner.CodeRunner;
import io.github.shaojunying.onlineexecutor.runner.RunResult;
import io.github.shaojunying.onlineexecutor.runner.RunStatus;

/**
 * Runs user code inside the server JVM, as in "Understanding the JVM", section 9.3.
 *
 * <p>Each run gets a new {@link ByteArrayClassLoader} and a new {@link ExecutionContext},
 * and runs {@code main} on its own daemon thread.
 *
 * <p>Limits of this mode:
 * <ul>
 *   <li>Java cannot stop a thread by force. After a timeout the runner interrupts the
 *       thread and makes every later print throw, but a loop that never prints keeps
 *       using CPU until the server restarts.</li>
 *   <li>User code can still read files, open sockets and start processes.</li>
 * </ul>
 * Use {@code process} or {@code docker} mode for code you do not trust.
 */
public class InProcessRunner implements CodeRunner {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private final Duration timeout;
    private final int maxOutputBytes;

    public InProcessRunner(Duration timeout, int maxOutputBytes) {
        this.timeout = timeout;
        this.maxOutputBytes = maxOutputBytes;
    }

    @Override
    public RunResult run(CompiledProgram program, String stdin) {
        ExecutionContext context = new ExecutionContext(stdin, maxOutputBytes);
        ByteArrayClassLoader loader = new ByteArrayClassLoader(program.classes());
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread worker = Thread.ofPlatform()
                .daemon()
                .name("user-code-" + THREAD_COUNTER.incrementAndGet())
                .unstarted(() -> {
                    context.bindToCurrentThread();
                    try {
                        invokeMain(loader, program.mainClassName());
                    } catch (Throwable t) {
                        failure.set(t);
                    } finally {
                        ExecutionContext.unbindCurrentThread();
                    }
                });
        worker.setContextClassLoader(loader);

        long start = System.nanoTime();
        worker.start();
        boolean finished = join(worker);
        context.close();
        long durationMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

        String stdout = context.stdout.content();
        String stderr = context.stderr.content();
        if (!finished) {
            worker.interrupt();
            return new RunResult(RunStatus.TIMEOUT, stdout, stderr, null, durationMillis);
        }
        return toResult(failure.get(), stdout, stderr, durationMillis);
    }

    private boolean join(Thread worker) {
        try {
            return worker.join(timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static RunResult toResult(Throwable failure, String stdout, String stderr, long durationMillis) {
        if (failure == null) {
            return RunResult.fromExitCode(0, stdout, stderr, durationMillis);
        }
        UserCodeStop stop = findStop(failure);
        if (stop != null) {
            return switch (stop.reason()) {
                case EXIT -> RunResult.fromExitCode(stop.exitCode(), stdout, stderr, durationMillis);
                case OUTPUT_LIMIT -> new RunResult(RunStatus.OUTPUT_LIMIT_EXCEEDED, stdout, stderr, null, durationMillis);
                case CANCELLED -> new RunResult(RunStatus.TIMEOUT, stdout, stderr, null, durationMillis);
            };
        }
        return RunResult.fromExitCode(1, stdout, stderr + describe(failure), durationMillis);
    }

    private static UserCodeStop findStop(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof UserCodeStop stop) {
                return stop;
            }
        }
        return null;
    }

    /** Formats an uncaught exception the way the {@code java} launcher does. */
    private static String describe(Throwable failure) {
        if (failure instanceof LaunchException launch) {
            return "Error: " + launch.getMessage() + System.lineSeparator();
        }
        StringWriter text = new StringWriter();
        text.append("Exception in thread \"main\" ");
        failure.printStackTrace(new PrintWriter(text));
        return text.toString();
    }

    /**
     * Finds and calls the entry point. Supports {@code static void main(String[])},
     * and also the instance and no-argument forms that Java 25 allows (JEP 512).
     */
    private static void invokeMain(ClassLoader loader, String className) throws Throwable {
        Class<?> type;
        try {
            type = Class.forName(className, true, loader);
        } catch (ClassNotFoundException e) {
            throw new LaunchException("Could not find or load main class " + className);
        } catch (ExceptionInInitializerError e) {
            throw e.getCause();
        }
        Method main = findMain(type);
        main.setAccessible(true);
        Object target = null;
        if (!Modifier.isStatic(main.getModifiers())) {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            target = constructor.newInstance();
        }
        try {
            if (main.getParameterCount() == 1) {
                main.invoke(target, (Object) new String[0]);
            } else {
                main.invoke(target);
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static Method findMain(Class<?> type) throws LaunchException {
        for (Class<?>[] parameters : new Class<?>[][]{{String[].class}, {}}) {
            try {
                Method method = type.getDeclaredMethod("main", parameters);
                if (!Modifier.isPrivate(method.getModifiers()) && method.getReturnType() == void.class) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // Try the next form.
            }
        }
        throw new LaunchException("Main method not found in class " + type.getName()
                + ", please define the main method as: public static void main(String[] args)");
    }

    /** The program could not start. */
    private static final class LaunchException extends Exception {
        LaunchException(String message) {
            super(message);
        }
    }
}
