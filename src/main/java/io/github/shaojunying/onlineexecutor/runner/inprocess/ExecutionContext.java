package io.github.shaojunying.onlineexecutor.runner.inprocess;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.github.shaojunying.onlineexecutor.runner.BoundedOutput;

/**
 * Per-run state that {@link HackSystem} reads through an inheritable thread-local.
 *
 * <p>Each run has its own stdin and output buffers, so two runs at the same time
 * never see each other's output. Threads that user code starts inherit the context.
 */
final class ExecutionContext {

    private static final InheritableThreadLocal<ExecutionContext> CURRENT = new InheritableThreadLocal<>();

    final InputStream stdin;
    final BoundedOutput stdout;
    final BoundedOutput stderr;
    private volatile boolean closed;

    ExecutionContext(String stdin, int maxOutputBytes) {
        this.stdin = new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8));
        this.stdout = new BoundedOutput(maxOutputBytes);
        this.stderr = new BoundedOutput(maxOutputBytes);
    }

    static ExecutionContext current() {
        return CURRENT.get();
    }

    void bindToCurrentThread() {
        CURRENT.set(this);
    }

    static void unbindCurrentThread() {
        CURRENT.remove();
    }

    /** After close, every write from user code throws, which stops most runaway loops. */
    void close() {
        closed = true;
    }

    boolean closed() {
        return closed;
    }

    boolean overflowed() {
        return stdout.overflowed() || stderr.overflowed();
    }
}
