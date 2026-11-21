package com.shao.execute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteUtilsTest {

    @Test
    void bytes2Int() {
        int num = ByteUtils.bytes2Int(new byte[]{(byte) 0xfe, (byte) 0xfa}, 0, 2);
        assertEquals(65274, num);
    }

    @Test
    void int2Bytes() {
        byte[] bytes = ByteUtils.int2Bytes(65274, 2);
        assertArrayEquals(new byte[]{(byte) 0xfe, (byte) 0xfa}, bytes);
    }

    @Test
    void bytes2String() {
        String originalStr = "北京邮电大学";
        String str = ByteUtils.bytes2String(originalStr.getBytes(), 0, originalStr.getBytes().length);
        assertEquals(originalStr, str);
    }

    @Test
    void string2Bytes() {
        String originalStr = "北京邮电大学";
        byte[] bytes = ByteUtils.string2Bytes(originalStr);
        assertArrayEquals(originalStr.getBytes(), bytes);
    }

    @Test
    void byteReplace() {
        byte[] originalBytes = new byte[]{(byte) 0xfe, (byte) 0xfa};
        byte[] replaceBytes = new byte[]{(byte) 0x11, (byte) 0xa1};
        byte[] res = ByteUtils.byteReplace(originalBytes, 0, 1, replaceBytes);
        assertArrayEquals(new byte[]{(byte) 0xfe, (byte) 0xfa}, originalBytes);
        assertArrayEquals(new byte[]{(byte) 0x11, (byte) 0xa1, (byte) 0xfa}, res);
    }
}
