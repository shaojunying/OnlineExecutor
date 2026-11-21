package io.github.shaojunying.onlineexecutor.runner.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.shaojunying.onlineexecutor.runner.CodeRunner;
import io.github.shaojunying.onlineexecutor.runner.CodeRunnerContractTest;
import io.github.shaojunying.onlineexecutor.runner.RunResult;
import io.github.shaojunying.onlineexecutor.runner.RunStatus;

class ProcessRunnerTest extends CodeRunnerContractTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(4);
    private final ProcessRunner runner = new ProcessRunner(TIMEOUT, MAX_OUTPUT_BYTES, 64);

    @Override
    protected CodeRunner runner() {
        return runner;
    }

    @Override
    protected Duration timeout() {
        return TIMEOUT;
    }

    /** In-process mode cannot stop this loop. A child process can be killed. */
    @Test
    void killsBusyLoop() {
        RunResult result = run("""
                public class Spin {
                    public static void main(String[] args) {
                        long n = 0;
                        while (true) {
                            n++;
                        }
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(RunStatus.TIMEOUT);
    }

    @Test
    void startsChildWithoutServerEnvironment() {
        RunResult result = run("""
                public class Env {
                    public static void main(String[] args) {
                        System.out.println(System.getenv("PATH") + " " + System.getenv("JAVA_HOME"));
                    }
                }
                """);

        assertThat(result.stdout()).isEqualTo("null null\n");
    }

    @Test
    void limitsHeap() {
        RunResult result = run("""
                public class Hog {
                    public static void main(String[] args) {
                        byte[][] blocks = new byte[1024][];
                        for (int i = 0; i < blocks.length; i++) {
                            blocks[i] = new byte[1024 * 1024];
                        }
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("java.lang.OutOfMemoryError");
    }
}
