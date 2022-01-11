package com.shao;

import java.io.*;
import java.nio.channels.Channel;
import java.util.Map;
import java.util.Properties;

/**
 * 该方法用来将代码的输出从 标准输出流 重定向到一个 新的输出流 中。
 *
 * 该方法将out和err重定向到了ByteArrayOutputStream对象中，
 * 并提供了两个方法获取和清理ByteArrayOutputStream对象中的内容，
 * 其余函数都是封装System的函数，内部直接调用System相应的方法。
 * @author shaojunying
 */
public final class HackSystem {

    /** Don't let anyone instantiate this class */
    private HackSystem() {

    }

    public final static InputStream in = null;

    public final static ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public final static PrintStream out = new PrintStream(buffer);

    public final static PrintStream err = out;

    private static volatile SecurityManager security = null;

    /**
     * 新定义的方法 获取buffer中的内容（out或err产生的）
     */
    public static String getBufferString() {
        return buffer.toString();
    }

    public static void clearBuffer() {
        buffer.reset();
    }


    /**
     * 下面的方法都还是封装了System中的方法
     * @param in
     */
    public static void setIn(InputStream in) {
        System.setIn(in);
    }

    public static void setOut(PrintStream out) {
        System.setOut(out);
    }

    public static void setErr(PrintStream err) {
        System.setErr(err);
    }

    private static volatile Console cons = null;

    public static Console console() {
        return System.console();
    }

    public static void setSecurityManager(SecurityManager s) {
        System.setSecurityManager(s);
    }

    public static SecurityManager getSecurityManager() {
        return System.getSecurityManager();
    }

    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    public static void setProperties(Properties props) {
        System.setProperties(props);
    }

    public static Properties getProperties() {
        return System.getProperties();
    }

    public static void setProperty(String key, String value) {
        System.setProperty(key, value);
    }

    public static String getProperty(String key) {
        return System.getProperty(key);
    }

    public static String getProperty(String key, String def) {
        return System.getProperty(key, def);
    }

    public static String clearProperty(String key) {
        return System.clearProperty(key);
    }

    public static String getenv(String name) {
        return System.getenv(name);
    }

    public static Map<String, String> getenv() {
        return System.getenv();
    }

    public static void exit(int status) {
        System.exit(status);
    }

    public static void gc() {
        System.gc();
    }

    public static void runFinalization() {
        System.runFinalization();
    }

    public static void runFinalizersOnExit(boolean value) {
        System.runFinalizersOnExit(value);
    }

    public static void load(String filename) {
        System.load(filename);
    }

    public static void loadLibrary(String libname) {
        System.loadLibrary(libname);
    }

    public static Channel inheritedChannel() throws IOException {
        return System.inheritedChannel();
    }


    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static long nanoTime() {
        return System.nanoTime();
    }

    public static int identityHashCode(Object x) {
        return System.identityHashCode(x);
    }

    public static String lineSeparator() {
        return System.lineSeparator();
    }

    public static String mapLibraryName(String libname) {
        return System.mapLibraryName(libname);
    }

}
