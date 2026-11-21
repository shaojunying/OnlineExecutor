package io.github.shaojunying.onlineexecutor.runner.inprocess;

import java.nio.charset.StandardCharsets;

/**
 * Helpers for reading and writing big-endian numbers and strings in a byte array.
 *
 * <p>Strings use UTF-8. A class file stores strings in modified UTF-8. The two encodings
 * are the same for the ASCII class names this project rewrites.
 */
public final class ByteUtils {

    private ByteUtils() {
    }

    /** Reads an unsigned big-endian number of {@code len} bytes. */
    public static int bytes2Int(byte[] bytes, int start, int len) {
        int num = 0;
        for (int i = start; i < start + len; i++) {
            num = (num << 8) | (bytes[i] & 0xff);
        }
        return num;
    }

    /** Writes {@code value} as a big-endian number of {@code len} bytes. */
    public static byte[] int2Bytes(int value, int len) {
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[len - 1 - i] = (byte) ((value >> 8 * i) & 0xff);
        }
        return bytes;
    }

    public static String bytes2String(byte[] bytes, int start, int len) {
        return new String(bytes, start, len, StandardCharsets.UTF_8);
    }

    public static byte[] string2Bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns a new array where {@code originalBytes[start, start + len)} is replaced by
     * {@code replaceBytes}. The input array is not changed.
     */
    public static byte[] byteReplace(byte[] originalBytes, int start, int len, byte[] replaceBytes) {
        byte[] bytes = new byte[originalBytes.length - len + replaceBytes.length];
        System.arraycopy(originalBytes, 0, bytes, 0, start);
        System.arraycopy(replaceBytes, 0, bytes, start, replaceBytes.length);
        System.arraycopy(originalBytes, start + len, bytes, start + replaceBytes.length,
                originalBytes.length - start - len);
        return bytes;
    }
}
