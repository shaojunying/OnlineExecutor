package io.github.shaojunying.onlineexecutor.runner;

/**
 * Outcome of one execution.
 */
public enum RunStatus {
    /** The program ended with exit code 0. */
    SUCCESS,
    /** The source did not compile. */
    COMPILE_ERROR,
    /** The program threw an uncaught exception or ended with a non-zero exit code. */
    RUNTIME_ERROR,
    /** The program ran longer than the configured timeout. */
    TIMEOUT,
    /** The program wrote more output than the configured limit. */
    OUTPUT_LIMIT_EXCEEDED
}
