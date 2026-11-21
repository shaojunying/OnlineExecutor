package io.github.shaojunying.onlineexecutor.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.github.shaojunying.onlineexecutor.compile.CompilationResult;
import io.github.shaojunying.onlineexecutor.compile.CompiledProgram;
import io.github.shaojunying.onlineexecutor.compile.InMemoryJavaCompiler;

/**
 * Behavior that every {@link CodeRunner} must have. Each runner has a subclass of this test.
 */
public abstract class CodeRunnerContractTest {

    protected static final int MAX_OUTPUT_BYTES = 10_000;
    private static final InMemoryJavaCompiler COMPILER = new InMemoryJavaCompiler();

    protected abstract CodeRunner runner();

    /** The timeout the runner from {@link #runner()} uses. */
    protected abstract Duration timeout();

    protected RunResult run(String source) {
        return run(source, "");
    }

    protected RunResult run(String source, String stdin) {
        RunResult result = runner().run(compile(source), stdin);
        return new RunResult(result.status(), normalize(result.stdout()), normalize(result.stderr()),
                result.exitCode(), result.durationMillis());
    }

    protected static CompiledProgram compile(String source) {
        CompilationResult result = COMPILER.compile(source);
        assertThat(result).isInstanceOf(CompilationResult.Success.class);
        return ((CompilationResult.Success) result).program();
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n");
    }

    @Test
    void printsToStdout() {
        RunResult result = run("""
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello, World");
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo("Hello, World\n");
    }

    /** Lambdas and string concatenation add invokedynamic constants, which broke the old ClassModifier. */
    @Test
    void runsLambdaAndStringConcatenation() {
        RunResult result = run("""
                import java.util.List;
                public class Modern {
                    public static void main(String[] args) {
                        int x = 41;
                        String text = "x=" + (x + 1);
                        List.of(1, 2, 3).forEach(n -> System.out.print(n));
                        System.out.println(" " + text);
                    }
                }
                """);

        assertThat(result.stdout()).isEqualTo("123 x=42\n");
    }

    @Test
    void runsNestedAndAnonymousClasses() {
        RunResult result = run("""
                public class Outer {
                    record Point(int x, int y) {}
                    static class Counter { int value = 7; }
                    public static void main(String[] args) {
                        Runnable r = new Runnable() {
                            public void run() { System.out.println(new Counter().value + " " + new Point(1, 2)); }
                        };
                        r.run();
                    }
                }
                """);

        assertThat(result.stdout()).isEqualTo("7 Point[x=1, y=2]\n");
    }

    @Test
    void runsClassInPackage() {
        RunResult result = run("""
                package demo;
                public class App { public static void main(String[] args) { System.out.println("pkg"); } }
                """);

        assertThat(result.stdout()).isEqualTo("pkg\n");
    }

    @Test
    void readsStdin() {
        RunResult result = run("""
                import java.util.Scanner;
                public class Sum {
                    public static void main(String[] args) {
                        Scanner in = new Scanner(System.in);
                        System.out.println(in.nextInt() + in.nextInt());
                    }
                }
                """, "40 2\n");

        assertThat(result.stdout()).isEqualTo("42\n");
    }

    @Test
    void reportsExitCode() {
        RunResult result = run("""
                public class Exit {
                    public static void main(String[] args) {
                        System.out.println("before");
                        System.exit(3);
                        System.out.println("after");
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).isEqualTo("before\n");
    }

    @Test
    void reportsUncaughtException() {
        RunResult result = run("""
                public class Boom {
                    public static void main(String[] args) {
                        throw new IllegalStateException("boom");
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stderr()).contains("Exception in thread \"main\" java.lang.IllegalStateException: boom");
    }

    @Test
    void reportsMissingMainMethod() {
        RunResult result = run("public class NoMain { }");

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("Main method not found in class NoMain");
    }

    @Test
    void stopsProgramAfterTimeout() {
        RunResult result = run("""
                public class Sleepy {
                    public static void main(String[] args) throws Exception {
                        System.out.println("started");
                        while (true) {
                            Thread.sleep(50);
                        }
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(RunStatus.TIMEOUT);
        assertThat(result.exitCode()).isNull();
        assertThat(result.stdout()).isEqualTo("started\n");
        assertThat(result.durationMillis()).isBetween(timeout().toMillis(), timeout().toMillis() + 5_000);
    }

    @Test
    void stopsProgramThatPrintsTooMuch() {
        RunResult result = run("""
                public class Flood {
                    public static void main(String[] args) {
                        while (true) {
                            System.out.println("x".repeat(100));
                        }
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(RunStatus.OUTPUT_LIMIT_EXCEEDED);
        assertThat(result.stdout()).hasSizeLessThanOrEqualTo(MAX_OUTPUT_BYTES + BoundedOutput.TRUNCATED_MARKER.length());
        assertThat(result.stdout()).endsWith(BoundedOutput.TRUNCATED_MARKER);
    }

    /** The old HackSystem used one static buffer for all requests. */
    @Test
    void keepsConcurrentRunsSeparate() throws Exception {
        List<String> sources = IntStream.range(0, 4).mapToObj(id -> """
                public class Worker {
                    public static void main(String[] args) {
                        for (int i = 0; i < 200; i++) {
                            System.out.println("%d");
                        }
                    }
                }
                """.formatted(id)).toList();

        try (ExecutorService pool = Executors.newFixedThreadPool(sources.size())) {
            List<Future<RunResult>> futures = sources.stream().map(s -> pool.submit(() -> run(s))).toList();
            for (int id = 0; id < sources.size(); id++) {
                String expected = (id + "\n").repeat(200);
                assertThat(futures.get(id).get().stdout()).isEqualTo(expected);
            }
        }
    }

    /** A new class loader per run lets the same class name load again with new content. */
    @Test
    void reloadsClassWithSameName() {
        assertThat(run("public class Same { public static void main(String[] a) { System.out.println(1); } }").stdout())
                .isEqualTo("1\n");
        assertThat(run("public class Same { public static void main(String[] a) { System.out.println(2); } }").stdout())
                .isEqualTo("2\n");
    }
}
