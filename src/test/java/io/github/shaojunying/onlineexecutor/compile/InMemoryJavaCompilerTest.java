package com.shao.compile;

import com.shao.execute.MyClassLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class StringSourceCompilerTest {

    @Test
    void compile() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String classContent =
                "public class Test { " +
                "   public static void main(String[] args) { " +
                "       System.out.println(\"Hello World!!!hhh\"); " +
                "   }" +
                "}";
        StringSourceCompiler compiler = new StringSourceCompiler();
        DiagnosticCollector<JavaFileObject> compileCollector = new DiagnosticCollector<>();
        byte[] bytes = compiler.compile(classContent, compileCollector);
        assertNotNull(bytes);

        MyClassLoader myClassLoader = new MyClassLoader();

        // 加载bytes
        Class<?> clazz = myClassLoader.loadByte(bytes);
        // 调用类中的方法
        Method method = new Method[]{clazz.getDeclaredMethod("main", String[].class)}[0];
        method.invoke(null, new Object[]{new String[]{}});

    }

    @Test
    void compileWhenNoClassDefined() {
        String classContent =
                "public static void main(String[] args) { " +
                "    System.out.println(\"Hello World!!!hhh\"); " +
                "}";
        StringSourceCompiler compiler = new StringSourceCompiler();
        DiagnosticCollector<JavaFileObject> compileCollector = new DiagnosticCollector<>();
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            compiler.compile(classContent, compileCollector);
        });
    }

    @Test
    void compileWithCompileError() {
        String classContent =
                "public class Test { " +
                "   public static void main(String[] args) { " +
                "       List<Integer> list = Arrays.asList(1, 2, 3);" +
                "       System.out.println(\"Hello World!!!hhh\"); " +
                "   }" +
                "}";
        StringSourceCompiler compiler = new StringSourceCompiler();
        DiagnosticCollector<JavaFileObject> compileCollector = new DiagnosticCollector<>();
        byte[] bytes = compiler.compile(classContent, compileCollector);
        assertNull(bytes);
        assertNotNull(compileCollector.getDiagnostics());
    }

    @Test
    @Disabled
    void testCompilerWithFileSource() throws IOException {
        File file = new File("/Users/shaojunying/Java-Test/com/shao/Test.java");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        compiler.getTask(null, fileManager, null, null, null, fileManager.getJavaFileObjects(file)).call();
        fileManager.close();
    }

}
