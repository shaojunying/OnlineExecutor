package io.github.shaojunying.onlineexecutor.runner.inprocess;

/**
 * Replaces a {@code CONSTANT_Utf8_info} string in the constant pool of a class file.
 *
 * <p>Class file layout (JVMS §4.1):
 * <pre>
 * [0, 3]  magic 0xCAFEBABE
 * [4, 7]  minor and major version
 * [8, 9]  constant_pool_count (u2)
 * [10...] constant pool entries, index 1 to constant_pool_count - 1
 * </pre>
 * A {@code CONSTANT_Utf8_info} entry is {@code tag(u1)=1, length(u2), bytes[length]}.
 * All other entries have a fixed size given by their tag (JVMS §4.4).
 * {@code CONSTANT_Long} and {@code CONSTANT_Double} use two pool indexes.
 *
 * <p>This is the technique from "Understanding the JVM", section 9.3.
 */
public class ClassModifier {

    private static final int MAGIC = 0xCAFEBABE;
    private static final int CONSTANT_POOL_COUNT_INDEX = 8;
    private static final int U1 = 1;
    private static final int U2 = 2;

    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;

    /** Size in bytes of each fixed-size entry, including the tag. 0 means the tag is not defined. */
    private static final int[] ENTRY_SIZE = new int[21];

    static {
        ENTRY_SIZE[3] = 5;   // Integer
        ENTRY_SIZE[4] = 5;   // Float
        ENTRY_SIZE[5] = 9;   // Long
        ENTRY_SIZE[6] = 9;   // Double
        ENTRY_SIZE[7] = 3;   // Class
        ENTRY_SIZE[8] = 3;   // String
        ENTRY_SIZE[9] = 5;   // Fieldref
        ENTRY_SIZE[10] = 5;  // Methodref
        ENTRY_SIZE[11] = 5;  // InterfaceMethodref
        ENTRY_SIZE[12] = 5;  // NameAndType
        ENTRY_SIZE[15] = 4;  // MethodHandle (Java 7)
        ENTRY_SIZE[16] = 3;  // MethodType (Java 7)
        ENTRY_SIZE[17] = 5;  // Dynamic (Java 11)
        ENTRY_SIZE[18] = 5;  // InvokeDynamic (Java 7): lambdas, string concatenation
        ENTRY_SIZE[19] = 3;  // Module (Java 9)
        ENTRY_SIZE[20] = 3;  // Package (Java 9)
    }

    private final byte[] bytes;

    public ClassModifier(byte[] bytes) {
        if (bytes.length < CONSTANT_POOL_COUNT_INDEX + U2 || ByteUtils.bytes2Int(bytes, 0, 4) != MAGIC) {
            throw new IllegalArgumentException("Not a class file");
        }
        this.bytes = bytes;
    }

    /**
     * Returns a copy of the class file where the UTF-8 constant {@code oldStr} is replaced
     * by {@code newStr}. Returns the original bytes when {@code oldStr} is not in the pool.
     */
    public byte[] modifyUTF8Constant(String oldStr, String newStr) {
        byte[] oldBytes = ByteUtils.string2Bytes(oldStr);
        int count = getConstantPoolCount();
        int offset = CONSTANT_POOL_COUNT_INDEX + U2;
        for (int index = 1; index < count; index++) {
            int tag = ByteUtils.bytes2Int(bytes, offset, U1);
            if (tag != CONSTANT_UTF8) {
                offset += entrySize(tag, offset);
                if (tag == CONSTANT_LONG || tag == CONSTANT_DOUBLE) {
                    index++;
                }
                continue;
            }
            int lengthOffset = offset + U1;
            int contentOffset = lengthOffset + U2;
            int length = ByteUtils.bytes2Int(bytes, lengthOffset, U2);
            if (regionEquals(contentOffset, length, oldBytes)) {
                byte[] newBytes = ByteUtils.string2Bytes(newStr);
                byte[] result = ByteUtils.byteReplace(bytes, contentOffset, length, newBytes);
                return ByteUtils.byteReplace(result, lengthOffset, U2, ByteUtils.int2Bytes(newBytes.length, U2));
            }
            offset = contentOffset + length;
        }
        return bytes;
    }

    public int getConstantPoolCount() {
        return ByteUtils.bytes2Int(bytes, CONSTANT_POOL_COUNT_INDEX, U2);
    }

    private static int entrySize(int tag, int offset) {
        int size = tag < ENTRY_SIZE.length ? ENTRY_SIZE[tag] : 0;
        if (size == 0) {
            throw new IllegalArgumentException("Unknown constant pool tag " + tag + " at offset " + offset);
        }
        return size;
    }

    private boolean regionEquals(int start, int length, byte[] expected) {
        if (length != expected.length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (bytes[start + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
