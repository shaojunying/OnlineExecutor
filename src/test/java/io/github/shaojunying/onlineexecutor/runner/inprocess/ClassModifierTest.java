package com.shao.execute;

import com.shao.execute.ClassModifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassModifierTest {

    /**
     * 从《深入理解Java虚拟机》中摘出来的
     */
    private static final byte[] bytes = new byte[]{
            (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x32,
            // 常量池大小为3
            (byte) 0x00, (byte) 0x03,
            // 下面是第一个常量的信息
            (byte) 0x07, (byte) 0x00,
            (byte) 0x02,
            // 下面首先是tag 后面 2 字节是长度
            (byte) 0x01, (byte) 0x00, (byte) 0x1D,
            // 从这里开始表示的是 org/fenixsoft/clazz/TestClass
            (byte) 0x6F, (byte) 0x72, (byte) 0x67, (byte) 0x2F,
            (byte) 0x66, (byte) 0x65, (byte) 0x6E, (byte) 0x69,
            (byte) 0x78, (byte) 0x73, (byte) 0x6F, (byte) 0x66,
            (byte) 0x74, (byte) 0x2F, (byte) 0x63, (byte) 0x6C,
            (byte) 0x61, (byte) 0x7A, (byte) 0x7A, (byte) 0x2F,
            (byte) 0x54, (byte) 0x65, (byte) 0x73, (byte) 0x74,
            (byte) 0x43, (byte) 0x6C, (byte) 0x61, (byte) 0x73,
            (byte) 0x73,
    };

    /**
     * 没有找到目标字符串
     */
    @Test
    void modifyUTF8ConstantWithoutTargetStr() {
        ClassModifier classModifier = new ClassModifier(bytes);
        final String oldStr = "org/fenixsoft/TestClass";
        final String newStr = "com/shao/Test";
        byte[] actualBytes = classModifier.modifyUTF8Constant(oldStr, newStr);
        assertArrayEquals(bytes, actualBytes);
    }

    /**
     * 找到目标字符串
     */
    @Test
    void modifyUTF8ConstantWithTargetStr() {
        ClassModifier classModifier = new ClassModifier(bytes);
        final String oldStr = "org/fenixsoft/clazz/TestClass";
        final String newStr = "com/shao/Test";
        byte[] bytes = classModifier.modifyUTF8Constant(oldStr, newStr);
        byte[] expectedBytes = new byte[]{
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x32,
                (byte) 0x00, (byte) 0x03, (byte) 0x07, (byte) 0x00,
                (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x0D,
                // 从这里开始表示的是 com/shao/Test
                (byte) 0x63, (byte) 0x6f, (byte) 0x6d, (byte) 0x2F,
                (byte) 0x73, (byte) 0x68, (byte) 0x61, (byte) 0x6f,
                (byte) 0x2f, (byte) 0x54, (byte) 0x65, (byte) 0x73,
                (byte) 0x74,
        };
        assertArrayEquals(expectedBytes, bytes);
    }
}
