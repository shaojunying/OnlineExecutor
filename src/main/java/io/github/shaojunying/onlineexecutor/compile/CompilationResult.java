package io.github.shaojunying.onlineexecutor.compile;

import java.util.List;

/**
 * Either a compiled program or the list of compiler errors.
 */
public sealed interface CompilationResult {

    record Success(CompiledProgram program) implements CompilationResult {
    }

    record Failure(List<CompileError> errors) implements CompilationResult {

        public Failure {
            errors = List.copyOf(errors);
        }
    }
}
