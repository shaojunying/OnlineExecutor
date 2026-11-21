package io.github.shaojunying.onlineexecutor.compile;

/**
 * One compiler error.
 *
 * @param line     1-based line, or 0 when javac gives no position
 * @param column   1-based column, or 0 when javac gives no position
 * @param message  javac message
 */
public record CompileError(long line, long column, String message) {

    @Override
    public String toString() {
        return line > 0 ? "line " + line + ":" + column + ": " + message : message;
    }
}
