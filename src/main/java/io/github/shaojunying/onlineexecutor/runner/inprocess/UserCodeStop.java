package io.github.shaojunying.onlineexecutor.runner.inprocess;

/**
 * Thrown inside user code to stop it.
 *
 * <p>It extends {@link Error}, so {@code catch (Exception e)} in user code does not swallow it.
 */
public final class UserCodeStop extends Error {

    public enum Reason { EXIT, OUTPUT_LIMIT, CANCELLED }

    private final Reason reason;
    private final int exitCode;

    private UserCodeStop(Reason reason, int exitCode, String message) {
        super(message, null, false, false);
        this.reason = reason;
        this.exitCode = exitCode;
    }

    static UserCodeStop exit(int status) {
        return new UserCodeStop(Reason.EXIT, status, "System.exit(" + status + ")");
    }

    static UserCodeStop outputLimit() {
        return new UserCodeStop(Reason.OUTPUT_LIMIT, 0, "Output limit exceeded");
    }

    static UserCodeStop cancelled() {
        return new UserCodeStop(Reason.CANCELLED, 0, "Execution cancelled");
    }

    public Reason reason() {
        return reason;
    }

    public int exitCode() {
        return exitCode;
    }
}
