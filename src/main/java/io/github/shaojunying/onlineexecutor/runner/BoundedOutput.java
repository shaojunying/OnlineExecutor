package io.github.shaojunying.onlineexecutor.runner;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A thread-safe byte buffer with a size limit.
 *
 * <p>When a write goes past the limit, the buffer keeps the bytes that fit,
 * drops the rest, and reports the overflow to the caller.
 */
public final class BoundedOutput {

    static final String TRUNCATED_MARKER = "\n... [output truncated]";

    private final int limit;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private boolean overflowed;

    public BoundedOutput(int limit) {
        this.limit = limit;
    }

    /**
     * @return {@code false} when the bytes did not fit and some were dropped
     */
    public synchronized boolean write(byte[] bytes, int offset, int length) {
        int room = limit - buffer.size();
        if (length <= room) {
            buffer.write(bytes, offset, length);
            return true;
        }
        buffer.write(bytes, offset, Math.max(room, 0));
        overflowed = true;
        return false;
    }

    public synchronized boolean overflowed() {
        return overflowed;
    }

    public synchronized String content() {
        String text = buffer.toString(StandardCharsets.UTF_8);
        return overflowed ? text + TRUNCATED_MARKER : text;
    }
}
