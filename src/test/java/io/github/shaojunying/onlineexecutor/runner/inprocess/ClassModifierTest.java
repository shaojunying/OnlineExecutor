package io.github.shaojunying.onlineexecutor.runner.inprocess;

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

    /** The length field must hold the byte length, not the character count. */
    @Test
    void writesByteLengthForMultiByteString() {
        byte[] result = new ClassModifier(bytes).modifyUTF8Constant("org/fenixsoft/clazz/TestClass", "测试");

        assertEquals(6, ByteUtils.bytes2Int(result, 14, 2));
        assertEquals("测试", ByteUtils.bytes2String(result, 16, 6));
        assertEquals(16 + 6, result.length);
    }

    @Test
    void rejectsBytesThatAreNotAClassFile() {
        assertThrows(IllegalArgumentException.class, () -> new ClassModifier(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
    }

    /** Lambdas and string concatenation add MethodHandle (15) and InvokeDynamic (18) constants. */
    @Test
    void rewritesClassWithInvokeDynamicConstants() {
        byte[] original = compile("""
                public class Modern {
                    public static void main(String[] args) {
                        Runnable r = () -> {};
                        String s = "n=" + args.length;
                        System.out.println(s);
                    }
                }
                """, "Modern");

        byte[] rewritten = new ClassModifier(original).modifyUTF8Constant("java/lang/System", "demo/Hack");

        assertTrue(containsUtf8Entry(original, "java/lang/System"));
        assertFalse(containsUtf8Entry(rewritten, "java/lang/System"));
        assertTrue(containsUtf8Entry(rewritten, "demo/Hack"));
    }

    /** Long and Double use two pool indexes. Walking the pool must not run past its end. */
    @Test
    void walksWholePoolWithLongAndDoubleConstants() {
        byte[] original = compile("""
                public class Numbers {
                    static long big = 123_456_789_012L;
                    static double pi = 3.14159;
                    static long other = 987_654_321_098L;
                }
                """, "Numbers");

        byte[] result = new ClassModifier(original).modifyUTF8Constant("not/in/Pool", "x");

        assertSame(original, result);
    }

    private static byte[] compile(String source, String className) {
        var result = new io.github.shaojunying.onlineexecutor.compile.InMemoryJavaCompiler().compile(source);
        var success = assertInstanceOf(io.github.shaojunying.onlineexecutor.compile.CompilationResult.Success.class, result);
        return success.program().classes().get(className);
    }

    /** True when the class file has a CONSTANT_Utf8 entry with exactly this value. */
    private static boolean containsUtf8Entry(byte[] classFile, String value) {
        byte[] text = ByteUtils.string2Bytes(value);
        byte[] entry = new byte[3 + text.length];
        entry[0] = 1;
        entry[1] = (byte) (text.length >> 8);
        entry[2] = (byte) text.length;
        System.arraycopy(text, 0, entry, 3, text.length);
        outer:
        for (int i = 0; i <= classFile.length - entry.length; i++) {
            for (int j = 0; j < entry.length; j++) {
                if (classFile[i + j] != entry[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
