package io.github.shaojunying.onlineexecutor.runner.inprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.github.shaojunying.onlineexecutor.runner.CodeRunner;
import io.github.shaojunying.onlineexecutor.runner.CodeRunnerContractTest;
import io.github.shaojunying.onlineexecutor.runner.RunResult;
import io.github.shaojunying.onlineexecutor.runner.RunStatus;

class InProcessRunnerTest extends CodeRunnerContractTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private final InProcessRunner runner = new InProcessRunner(TIMEOUT, MAX_OUTPUT_BYTES);

    @Override
    protected CodeRunner runner() {
        return runner;
    }

    @Override
    protected Duration timeout() {
        return TIMEOUT;
    }

    @Test
    void hidesServerEnvironment() {
        RunResult result = run("""
                public class Env {
                    public static void main(String[] args) {
                        System.out.println(System.getenv().size() + " " + System.getenv("PATH"));
                    }
                }
                """);

        assertThat(result.stdout()).isEqualTo("0 null\n");
    }

    @Test
    void deniesChangesToJvmWideState() {
        RunResult result = run("""
                public class Props {
                    public static void main(String[] args) {
                        System.setProperty("user.dir", "/");
                    }
                }
                """);

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("java.lang.SecurityException: System.setProperty is not allowed");
        assertThat(System.getProperty("user.dir")).isNotEqualTo("/");
    }

    @Test
    void hidesServerClasses() {
        RunResult result = run("""
                public class Peek {
                    public static void main(String[] args) {
                        try {
                            Class.forName("org.springframework.boot.SpringApplication");
                            System.out.println("visible");
                        } catch (ClassNotFoundException e) {
                            System.out.println("hidden");
                        }
                    }
                }
                """);

        assertThat(result.stdout()).isEqualTo("hidden\n");
    }

    @Test
    void doesNotLetCatchExceptionSwallowExit() {
        RunResult result = run("""
                public class Swallow {
                    public static void main(String[] args) {
                        try {
                            System.exit(4);
                        } catch (Exception e) {
                            System.out.println("swallowed");
                        }
                    }
                }
                """);

        assertThat(result.exitCode()).isEqualTo(4);
        assertThat(result.stdout()).isEmpty();
    }
}
