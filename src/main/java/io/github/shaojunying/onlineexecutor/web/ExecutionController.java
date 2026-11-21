package io.github.shaojunying.onlineexecutor.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.shaojunying.onlineexecutor.config.ExecutorProperties;
import io.github.shaojunying.onlineexecutor.service.ExecutionResult;
import io.github.shaojunying.onlineexecutor.service.ExecutionService;

/**
 * JSON API.
 *
 * <ul>
 *   <li>{@code POST /api/executions}: compile and run a source file.</li>
 *   <li>{@code GET /api/runtime}: the execution mode and limits, for the web page.</li>
 * </ul>
 * Compile errors and runtime errors are normal results (HTTP 200). HTTP 400 means the
 * request is invalid, HTTP 429 means the server is busy.
 */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExecutionController {

    private final ExecutionService executionService;
    private final ExecutorProperties properties;

    public ExecutionController(ExecutionService executionService, ExecutorProperties properties) {
        this.executionService = executionService;
        this.properties = properties;
    }

    @PostMapping(path = "/executions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ExecutionResult execute(@RequestBody ExecutionRequest request) {
        return executionService.execute(request.source(), request.stdin());
    }

    @GetMapping("/runtime")
    public RuntimeInfo runtime() {
        return new RuntimeInfo(
                properties.mode().name(),
                Runtime.version().feature(),
                properties.timeout().toMillis(),
                properties.maxSourceLength(),
                properties.maxOutputBytes());
    }

    /**
     * @param source  Java source code
     * @param stdin   standard input for the program; optional
     */
    public record ExecutionRequest(String source, String stdin) {
    }

    public record RuntimeInfo(String mode, int javaVersion, long timeoutMillis, int maxSourceLength, int maxOutputBytes) {
    }
}
