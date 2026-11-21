package io.github.shaojunying.onlineexecutor.service;

import java.util.List;

import io.github.shaojunying.onlineexecutor.compile.CompileError;
import io.github.shaojunying.onlineexecutor.runner.RunResult;
import io.github.shaojunying.onlineexecutor.runner.RunStatus;

/**
 * Result of compiling and running one source file. This is the JSON body of the API.
 *
 * @param status          outcome
 * @param stdout          standard output of the program
 * @param stderr          standard error of the program
 * @param exitCode        exit code, or {@code null} when the program did not run or did not end by itself
 * @param durationMillis  run time, without compile time
 * @param compileErrors   compiler errors when {@code status} is {@link RunStatus#COMPILE_ERROR}
 */
public record ExecutionResult(
        RunStatus status,
        String stdout,
        String stderr,
        Integer exitCode,
        long durationMillis,
        List<CompileError> compileErrors) {

    static ExecutionResult compileError(List<CompileError> errors) {
        return new ExecutionResult(RunStatus.COMPILE_ERROR, "", "", null, 0, errors);
    }

    static ExecutionResult of(RunResult run) {
        return new ExecutionResult(run.status(), run.stdout(), run.stderr(), run.exitCode(),
                run.durationMillis(), List.of());
    }
}
