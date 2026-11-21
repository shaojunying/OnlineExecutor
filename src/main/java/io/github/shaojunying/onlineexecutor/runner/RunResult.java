package io.github.shaojunying.onlineexecutor.runner;

/**
 * Result of running a compiled program.
 *
 * @param status          outcome
 * @param stdout          captured standard output
 * @param stderr          captured standard error
 * @param exitCode        exit code, or {@code null} when the program did not end by itself
 * @param durationMillis  wall-clock run time
 */
public record RunResult(RunStatus status, String stdout, String stderr, Integer exitCode, long durationMillis) {

    public static RunResult fromExitCode(int exitCode, String stdout, String stderr, long durationMillis) {
        RunStatus status = exitCode == 0 ? RunStatus.SUCCESS : RunStatus.RUNTIME_ERROR;
        return new RunResult(status, stdout, stderr, exitCode, durationMillis);
    }
}
