package io.github.shaojunying.onlineexecutor.runner.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.shaojunying.onlineexecutor.config.ExecutorProperties;
import io.github.shaojunying.onlineexecutor.runner.CodeRunner;
import io.github.shaojunying.onlineexecutor.runner.CodeRunnerContractTest;
import io.github.shaojunying.onlineexecutor.runner.RunResult;

/**
 * Runs only when a Docker daemon is reachable and the image is present.
 * CI pulls the image first; locally run {@code docker pull eclipse-temurin:<Java version>-jre}.
 */
@EnabledIf("dockerAvailable")
class DockerRunnerTest extends CodeRunnerContractTest {

    private static final String IMAGE = ExecutorProperties.Docker.defaultImage();
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private final DockerRunner runner = new DockerRunner(TIMEOUT, MAX_OUTPUT_BYTES, 64,
            new ExecutorProperties.Docker("docker", IMAGE, 256, "1.0", 64));

    @Override
    protected CodeRunner runner() {
        return runner;
    }

    @Override
    protected Duration timeout() {
        return TIMEOUT;
    }

    static boolean dockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "image", "inspect", IMAGE)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void blocksNetwork() {
        RunResult result = run("""
                public class Net {
                    public static void main(String[] args) {
                        try (var socket = new java.net.Socket()) {
                            socket.connect(new java.net.InetSocketAddress("1.1.1.1", 443), 2000);
                            System.out.println("connected");
                        } catch (Exception e) {
                            System.out.println("blocked");
                        }
                    }
                }
                """);

        assertThat(result.stdout()).isEqualTo("blocked\n");
    }

    @Test
    void mountsCodeReadOnly() {
        RunResult result = run("""
                public class Write {
                    public static void main(String[] args) {
                        try {
                            java.nio.file.Files.writeString(java.nio.file.Path.of("/code/x.txt"), "x");
                            System.out.println("written");
                        } catch (Exception e) {
                            System.out.println("read-only");
                        }
                    }
                }
                """);

        assertThat(result.stdout()).isEqualTo("read-only\n");
    }
}
