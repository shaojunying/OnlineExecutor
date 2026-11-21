package io.github.shaojunying.onlineexecutor.runner.inprocess;

import java.util.Map;

/**
 * Loads the classes of one user program from memory.
 *
 * <p>A new loader is created for every run, so the same class name can be loaded again
 * with new content.
 *
 * <p>Class lookup:
 * <ul>
 *   <li>{@link HackSystem} and its helpers come from the loader that loaded this class.
 *       In a Spring Boot fat jar that is the Boot class loader, not the system class loader.</li>
 *   <li>User classes come from the compiled bytes, after {@link ClassModifier} rewrites
 *       {@code java/lang/System} to {@link HackSystem}.</li>
 *   <li>Everything else comes from the platform class loader, so user code sees the JDK
 *       but not the server's own classes or libraries.</li>
 * </ul>
 */
public class ByteArrayClassLoader extends ClassLoader {

    static final String SYSTEM_INTERNAL_NAME = "java/lang/System";
    static final String HACK_SYSTEM_INTERNAL_NAME = HackSystem.class.getName().replace('.', '/');

    private static final String HACK_SYSTEM_NAME = HackSystem.class.getName();
    private static final ClassLoader SERVER_LOADER = HackSystem.class.getClassLoader();

    private final Map<String, byte[]> classes;

    public ByteArrayClassLoader(Map<String, byte[]> classes) {
        super("user-code", ClassLoader.getPlatformClassLoader());
        this.classes = classes;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.equals(HACK_SYSTEM_NAME) || name.startsWith(HACK_SYSTEM_NAME + "$")) {
            return SERVER_LOADER.loadClass(name);
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] original = classes.get(name);
        if (original == null) {
            throw new ClassNotFoundException(name);
        }
        byte[] rewritten = new ClassModifier(original)
                .modifyUTF8Constant(SYSTEM_INTERNAL_NAME, HACK_SYSTEM_INTERNAL_NAME);
        return defineClass(name, rewritten, 0, rewritten.length);
    }
}
