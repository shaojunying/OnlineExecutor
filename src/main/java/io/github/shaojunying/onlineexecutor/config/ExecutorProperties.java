package io.github.shaojunying.onlineexecutor.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for compiling and running user code. Prefix: {@code executor}.
 *
 * @param mode             how user code runs, see {@link RunnerMode}
 * @param timeout          wall-clock limit for one run
 * @param maxSourceLength  maximum source length in characters
 * @param maxStdinLength   maximum stdin length in characters
 * @param maxOutputBytes   maximum bytes kept for stdout, and separately for stderr
 * @param maxConcurrent    maximum number of runs at the same time; more requests get HTTP 429
 * @param heapMb           maximum heap of the child JVM (process and docker mode)
 * @param docker           settings for docker mode
 */
@ConfigurationProperties("executor")
public record ExecutorProperties(
        @DefaultValue("process") RunnerMode mode,
        @DefaultValue("10s") Duration timeout,
        @DefaultValue("65536") int maxSourceLength,
        @DefaultValue("65536") int maxStdinLength,
        @DefaultValue("65536") int maxOutputBytes,
        @DefaultValue("4") int maxConcurrent,
        @DefaultValue("128") int heapMb,
        @DefaultValue Docker docker) {

    /**
     * @param executable  docker CLI to call
     * @param image       image that contains a Java runtime. Empty means
     *                    {@code eclipse-temurin:<server Java version>-jre}. The image must run class
     *                    files of the server's Java version, because the server compiles the user code.
     * @param memoryMb    container memory limit; must be larger than {@code heapMb}
     * @param cpus        CPU limit
     * @param pidsLimit   maximum number of processes and threads in the container
     */
    public record Docker(
            @DefaultValue("docker") String executable,
            @DefaultValue("") String image,
            @DefaultValue("384") int memoryMb,
            @DefaultValue("1.0") String cpus,
            @DefaultValue("128") int pidsLimit) {

        public Docker {
            if (image == null || image.isBlank()) {
                image = defaultImage();
            }
        }

        /** A JRE image with the same feature version as the server. */
        public static String defaultImage() {
            return "eclipse-temurin:" + Runtime.version().feature() + "-jre";
        }
    }

    public enum RunnerMode {
        /** Run in the server JVM, with the class-file rewrite from the book. Not isolated. */
        IN_PROCESS,
        /** Run in a child JVM. Isolates crashes and enforces timeouts. Does not isolate files or network. */
        PROCESS,
        /** Run in a child JVM inside a locked-down container. Use this mode for public deployments. */
        DOCKER
    }
}
