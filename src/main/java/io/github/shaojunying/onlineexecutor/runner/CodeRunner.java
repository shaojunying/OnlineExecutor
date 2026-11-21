package io.github.shaojunying.onlineexecutor.runner;

import io.github.shaojunying.onlineexecutor.compile.CompiledProgram;

/**
 * Runs a compiled program and captures its output.
 *
 * <p>Implementations must be thread-safe. They must stop waiting for the program
 * when the timeout ends, and they must never let one run see the output of another run.
 */
public interface CodeRunner {

    /**
     * @param program  classes produced by the compiler
     * @param stdin    text given to the program as standard input
     */
    RunResult run(CompiledProgram program, String stdin);
}
