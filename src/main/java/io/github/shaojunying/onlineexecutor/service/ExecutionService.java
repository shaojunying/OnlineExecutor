package io.github.shaojunying.onlineexecutor.service;

import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Service;

import io.github.shaojunying.onlineexecutor.compile.CompilationResult;
import io.github.shaojunying.onlineexecutor.compile.InMemoryJavaCompiler;
import io.github.shaojunying.onlineexecutor.config.ExecutorProperties;
import io.github.shaojunying.onlineexecutor.runner.CodeRunner;

/**
 * Checks the request, compiles the source and runs it.
 *
 * <p>A semaphore limits how many requests compile or run at the same time.
 * A request that finds no free slot fails at once with {@link ExecutorBusyException}.
 */
@Service
public class ExecutionService {

    private final InMemoryJavaCompiler compiler;
    private final CodeRunner runner;
    private final ExecutorProperties properties;
    private final Semaphore slots;

    public ExecutionService(InMemoryJavaCompiler compiler, CodeRunner runner, ExecutorProperties properties) {
        this.compiler = compiler;
        this.runner = runner;
        this.properties = properties;
        this.slots = new Semaphore(properties.maxConcurrent());
    }

    public ExecutionResult execute(String source, String stdin) {
        String input = stdin == null ? "" : stdin;
        validate(source, input);
        if (!slots.tryAcquire()) {
            throw new ExecutorBusyException(properties.maxConcurrent());
        }
        try {
            return switch (compiler.compile(source)) {
                case CompilationResult.Failure failure -> ExecutionResult.compileError(failure.errors());
                case CompilationResult.Success success -> ExecutionResult.of(runner.run(success.program(), input));
            };
        } finally {
            slots.release();
        }
    }

    private void validate(String source, String stdin) {
        if (source == null || source.isBlank()) {
            throw new InvalidSourceException("Source code is empty.");
        }
        if (source.length() > properties.maxSourceLength()) {
            throw new InvalidSourceException("Source code is longer than "
                    + properties.maxSourceLength() + " characters.");
        }
        if (stdin.length() > properties.maxStdinLength()) {
            throw new InvalidSourceException("Standard input is longer than "
                    + properties.maxStdinLength() + " characters.");
        }
    }
}
