package com.shao;

import org.jetbrains.annotations.NotNull;

/**
 *
 * 字节数组处理的工具类
 *
 * @author shaojunying
 */
public class ByteUtils {

    public static int bytes2Int(byte[] bytes, int start, int len) {
        int num = 0;
        int end = start + len;
        for (int i = start; i < end; i++) {
            int n = ((int) bytes[i]) & 0xff;
            n <<= (-- len) * 8;
            num += n;
        }
        return num;
    }

    public static byte @NotNull [] int2Bytes(int value, int len) {
        byte[] bytes = new byte[len];
        for (int i = 0; i < bytes.length; i++) {
            bytes[len - 1 - i] = (byte) ((value >> 8 * i) & 0xff);
        }
        return bytes;
    }

    public static String bytes2String(byte[] bytes, int start, int len) {
        return new String(bytes, start, len);
    }

    public static byte[] string2Bytes(String value) {
        return value.getBytes();
    }

    /**
     * 将originalBytes[start, start + len)之间的字符替换为replaceBytes[0, replaceBytes.length)
     * 按照如下方式拼接一个长度为originalBytes.length - len + replaceBytes.length的新字符串：
     * originalBytes[0, start),
     * replaceBytes[0, replaceBytes.length),
     * originalBytes[start+len, originalBytes.length - start - len)
     */
    public static byte[] byteReplace(byte[] originalBytes, int start, int len, byte[] replaceBytes) {
        byte[] bytes = new byte[originalBytes.length - len + replaceBytes.length];
        System.arraycopy(originalBytes, 0, bytes, 0, start);
        System.arraycopy(replaceBytes, 0, bytes, start, replaceBytes.length);
        System.arraycopy(originalBytes, start + len, bytes, replaceBytes.length + start
                , originalBytes.length - start - len);
        return bytes;
    }


}
