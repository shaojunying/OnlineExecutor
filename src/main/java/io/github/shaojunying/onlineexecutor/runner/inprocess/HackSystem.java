package io.github.shaojunying.onlineexecutor.runner.inprocess;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.Channel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import io.github.shaojunying.onlineexecutor.runner.BoundedOutput;

/**
 * Replacement for {@link System} in user code that runs in the server JVM.
 *
 * <p>{@link ClassModifier} rewrites every reference to {@code java/lang/System} in user
 * class files to this class. This class must therefore declare every public static
 * member of {@link System}; {@code HackSystemTest} checks this.
 *
 * <p>Behavior:
 * <ul>
 *   <li>{@code in}, {@code out} and {@code err} use the {@link ExecutionContext} of the
 *       current thread, so concurrent runs stay separate.</li>
 *   <li>{@code exit} stops the user code instead of the server.</li>
 *   <li>Methods that change JVM-wide state ({@code setOut}, {@code setProperty},
 *       {@code loadLibrary}, ...) throw {@link SecurityException}.</li>
 *   <li>{@code getenv} returns nothing, so server secrets do not leak.</li>
 * </ul>
 * This is damage control, not a sandbox. Use process or docker mode for untrusted code.
 */
public final class HackSystem {

    private HackSystem() {
    }

    public static final InputStream in = new RoutedInputStream();

    public static final PrintStream out = new PrintStream(new RoutedOutputStream(false), true, StandardCharsets.UTF_8);

    public static final PrintStream err = new PrintStream(new RoutedOutputStream(true), true, StandardCharsets.UTF_8);

    private static final PrintStream SERVER_OUT = System.out;
    private static final PrintStream SERVER_ERR = System.err;

    // ---- Methods with changed behavior ----

    public static void exit(int status) {
        throw UserCodeStop.exit(status);
    }

    public static void setIn(InputStream in) {
        throw denied("setIn");
    }

    public static void setOut(PrintStream out) {
        throw denied("setOut");
    }

    public static void setErr(PrintStream err) {
        throw denied("setErr");
    }

    @Deprecated
    public static void setSecurityManager(SecurityManager s) {
        throw new UnsupportedOperationException("The Security Manager is not supported");
    }

    @Deprecated
    public static SecurityManager getSecurityManager() {
        return null;
    }

    public static Properties getProperties() {
        Properties copy = new Properties();
        copy.putAll(System.getProperties());
        return copy;
    }

    public static void setProperties(Properties props) {
        throw denied("setProperties");
    }

    public static String setProperty(String key, String value) {
        throw denied("setProperty");
    }

    public static String clearProperty(String key) {
        throw denied("clearProperty");
    }

    public static String getenv(String name) {
        return null;
    }

    public static Map<String, String> getenv() {
        return Map.of();
    }

    public static void load(String filename) {
        throw denied("load");
    }

    public static void loadLibrary(String libname) {
        throw denied("loadLibrary");
    }

    public static Console console() {
        return null;
    }

    public static Channel inheritedChannel() throws IOException {
        return null;
    }

    @Deprecated
    public static void runFinalization() {
        // Finalization is deprecated for removal. Nothing to do.
    }

    // ---- Methods that call System directly ----

    public static String getProperty(String key) {
        return System.getProperty(key);
    }

    public static String getProperty(String key, String def) {
        return System.getProperty(key, def);
    }

    public static System.Logger getLogger(String name) {
        return System.getLogger(name);
    }

    public static System.Logger getLogger(String name, ResourceBundle bundle) {
        return System.getLogger(name, bundle);
    }

    public static void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
        System.arraycopy(src, srcPos, dest, destPos, length);
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

    public static void gc() {
        System.gc();
    }

    private static SecurityException denied(String method) {
        return new SecurityException("System." + method + " is not allowed in in-process mode");
    }

    /** Writes to the current run, or to the server console when no run is bound to the thread. */
    private static final class RoutedOutputStream extends OutputStream {

        private final boolean error;

        RoutedOutputStream(boolean error) {
            this.error = error;
        }

        @Override
        public void write(int b) {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            ExecutionContext context = ExecutionContext.current();
            if (context == null) {
                (error ? SERVER_ERR : SERVER_OUT).write(b, off, len);
                return;
            }
            if (context.closed()) {
                throw UserCodeStop.cancelled();
            }
            BoundedOutput target = error ? context.stderr : context.stdout;
            if (!target.write(b, off, len)) {
                throw UserCodeStop.outputLimit();
            }
        }
    }

    /** Reads the stdin of the current run. */
    private static final class RoutedInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            ExecutionContext context = ExecutionContext.current();
            return context == null ? -1 : context.stdin.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            ExecutionContext context = ExecutionContext.current();
            return context == null ? -1 : context.stdin.read(b, off, len);
        }

        @Override
        public int available() throws IOException {
            ExecutionContext context = ExecutionContext.current();
            return context == null ? 0 : context.stdin.available();
        }
    }
}
