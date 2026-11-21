package io.github.shaojunying.onlineexecutor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.github.shaojunying.onlineexecutor.compile.InMemoryJavaCompiler;
import io.github.shaojunying.onlineexecutor.config.ExecutorProperties;
import io.github.shaojunying.onlineexecutor.config.ExecutorProperties.RunnerMode;
import io.github.shaojunying.onlineexecutor.runner.CodeRunner;
import io.github.shaojunying.onlineexecutor.runner.RunResult;
import io.github.shaojunying.onlineexecutor.runner.RunStatus;

class ExecutionServiceTest {

    private static final String HELLO = "public class A { public static void main(String[] a) {} }";
    private static final InMemoryJavaCompiler COMPILER = new InMemoryJavaCompiler();

    private final AtomicInteger runs = new AtomicInteger();
    private final CodeRunner countingRunner = (program, stdin) -> {
        runs.incrementAndGet();
        return new RunResult(RunStatus.SUCCESS, "out:" + stdin, "", 0, 1);
    };

    @Test
    void compilesAndRuns() {
        ExecutionResult result = service(countingRunner, 4).execute(HELLO, "in");

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("out:in");
        assertThat(result.compileErrors()).isEmpty();
    }

    @Test
    void treatsMissingStdinAsEmpty() {
        assertThat(service(countingRunner, 4).execute(HELLO, null).stdout()).isEqualTo("out:");
    }

    @Test
    void returnsCompileErrorsWithoutRunning() {
        ExecutionResult result = service(countingRunner, 4).execute("public class A { int x = ; }", "");

        assertThat(result.status()).isEqualTo(RunStatus.COMPILE_ERROR);
        assertThat(result.compileErrors()).isNotEmpty();
        assertThat(runs).hasValue(0);
    }

    @Test
    void rejectsBlankSource() {
        assertThatThrownBy(() -> service(countingRunner, 4).execute("  ", ""))
                .isInstanceOf(InvalidSourceException.class);
    }

    @Test
    void rejectsSourceOverLimit() {
        assertThatThrownBy(() -> service(countingRunner, 4).execute("x".repeat(101), ""))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("100 characters");
    }

    @Test
    void rejectsStdinOverLimit() {
        assertThatThrownBy(() -> service(countingRunner, 4).execute(HELLO, "x".repeat(51)))
                .isInstanceOf(InvalidSourceException.class)
                .hasMessageContaining("50 characters");
    }

    @Test
    void rejectsRequestWhenAllSlotsAreBusy() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CodeRunner blockingRunner = (program, stdin) -> {
            started.countDown();
            await(release);
            return new RunResult(RunStatus.SUCCESS, "", "", 0, 1);
        };
        ExecutionService service = service(blockingRunner, 1);

        Thread first = Thread.ofVirtual().start(() -> service.execute(HELLO, ""));
        assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> service.execute(HELLO, "")).isInstanceOf(ExecutorBusyException.class);

        release.countDown();
        first.join();
        assertThat(service.execute(HELLO, "").status()).isEqualTo(RunStatus.SUCCESS);
    }

    private static ExecutionService service(CodeRunner runner, int maxConcurrent) {
        ExecutorProperties properties = new ExecutorProperties(RunnerMode.PROCESS, Duration.ofSeconds(5),
                100, 50, 1000, maxConcurrent, 64,
                new ExecutorProperties.Docker("docker", "image", 256, "1.0", 64));
        return new ExecutionService(COMPILER, runner, properties);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
