package io.github.shaojunying.onlineexecutor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.shaojunying.onlineexecutor.runner.CodeRunner;
import io.github.shaojunying.onlineexecutor.runner.inprocess.InProcessRunner;
import io.github.shaojunying.onlineexecutor.runner.process.DockerRunner;
import io.github.shaojunying.onlineexecutor.runner.process.ProcessRunner;

/**
 * Creates the {@link CodeRunner} for the configured {@code executor.mode}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ExecutorProperties.class)
public class RunnerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RunnerConfiguration.class);

    @Bean
    CodeRunner codeRunner(ExecutorProperties p) {
        log.info("Execution mode: {}, timeout: {}", p.mode(), p.timeout());
        return switch (p.mode()) {
            case IN_PROCESS -> {
                log.warn("In-process mode runs user code inside the server JVM without isolation. "
                        + "Do not expose this server to untrusted users.");
                yield new InProcessRunner(p.timeout(), p.maxOutputBytes());
            }
            case PROCESS -> new ProcessRunner(p.timeout(), p.maxOutputBytes(), p.heapMb());
            case DOCKER -> new DockerRunner(p.timeout(), p.maxOutputBytes(), p.heapMb(), p.docker());
        };
    }
}
