package com.shao;

/**
 *
 * 修改Class文件中的常量池
 * [0,3] 字节为魔数
 * [4,7] 字节存两个版本号
 * [8]   常量池大小
 * [9]   第0项常量池 为0
 * [10,...] 各种类型常量
 * 假设从n位开始存储的是CONSTANT_Utf8_info，
 *      那么第n位存储tag = 1,
 *      [n + 1, n + 2]存储字符串长度len
 *      [n + 3, n + 3 + len - 1] 存储字符串内容
 * @author shaojunying
 */
public class ClassModifier {

    /**
     * 常量池的起始下标。这一位存储常量池大小
     */
    private static final int CONSTANT_POOL_COUNT_INDEX = 8;

    /**
     * CONSTANT_Utf8_info型常量的tag值
     */
    private static final int CONSTANT_UTF8_INFO_TAG = 1;

    /**
     * 常量池中11种常量所占的长度，CONSTANT_Utf8_info型常量除外，因为它不是定长的
     */
    private static final int[] CONSTANT_ITEM_LENGTH = { -1, -1, -1, 5, 5, 9, 9, 3, 3, 5, 5, 5, 5 };

    /**
     * tag占1字节 len占两字节 提前声明为常量
     */
    private static final int U1 = 1;
    private static final int U2 = 2;

    private byte[] bytes;

    public ClassModifier(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * 将bytes的UTF8常量池中oldStr修改为newStr，并返回修改后的结果
     * @param oldStr 修改前的字符串
     * @param newStr 修改后的字符串
     * @return 修改后的结果
     */
    public byte[] modifyUTF8Constant(String oldStr, String newStr) {
        // 获取常量池长度
        int constantPoolCount = getConstantPoolCount();
        int offset = CONSTANT_POOL_COUNT_INDEX + U2;
        for (int i = 1; i < constantPoolCount; i++) {
            int tag = ByteUtils.bytes2Int(bytes, offset, U1);
            if (tag != CONSTANT_UTF8_INFO_TAG) {
                offset += CONSTANT_ITEM_LENGTH[tag];
                continue;
            }
            // 是UTF8格式常量
            // 获取常量的内容
            offset += (U1 + U2);
            int len = ByteUtils.bytes2Int(bytes, offset - U2, U2);
            String str = ByteUtils.bytes2String(bytes, offset, len);
            if (!str.equals(oldStr)) {
                offset += len;
                continue;
            }
            // 匹配到oldStr
            // 新字符串长度、内容的字节表示
            byte[] newLenBytes = ByteUtils.int2Bytes(newStr.length(), U2);
            byte[] newContentBytes = ByteUtils.string2Bytes(newStr);

            // 替换长度 和 内容
            bytes = ByteUtils.byteReplace(bytes, offset - U2, U2, newLenBytes);
            bytes = ByteUtils.byteReplace(bytes, offset, len, newContentBytes);

            return bytes;
        }
        return bytes;
    }

    /**
     * 获取常量池中常量的数量
     */
    public int getConstantPoolCount() {
        return ByteUtils.bytes2Int(bytes, CONSTANT_POOL_COUNT_INDEX, U2);
    }

}
