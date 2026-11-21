package com.shao.compile;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;


/**
 * 参考链接：
 * https://stackoverflow.com/questions/12173294/compile-code-fully-in-memory-with-javax-tools-javacompiler
 * http://javapracs.blogspot.com/2011/06/dynamic-in-memory-compilation-using.html
 *
 * @author shaojunying
 */
public class StringSourceCompiler {

    /**
     * 将字符串中的Java代码编译为字节码
     * @param source 要编译的Java代码
     * @return 生成的字节码
     */
    public byte[] compile(String source, DiagnosticCollector<JavaFileObject> compileCollector) {

        String className = extractClassName(source);

        if (className.length() == 0) {
            throw new IllegalArgumentException("source code must contain class definition");
        }

        // 根据Class Name + Java 代码构造Java文件对象
        JavaFileObject javaFileObject = new JavaSourceFromString(className, source);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ClassFileManager classFileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
        // 只需要编译一个Java文件
        List<JavaFileObject> compilationUnits = Collections.singletonList(javaFileObject);

        compiler.getTask(null, classFileManager, compileCollector, null, null, compilationUnits)
                .call();

        return classFileManager.getBytes();
    }

    /**
     * 从代码中提取类名
     */
    private String extractClassName(String source) {
        int start = source.indexOf("class ");
        if (start == -1) {
            return "";
        }
        int end = source.indexOf(" ", start + 6);
        return source.substring(start + 6, end);
    }

    /**
     * 该类用于构造JavaFileObject，传输Java文件
     */
    private static class JavaSourceFromString extends SimpleJavaFileObject {

        private final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        /**
         * 获取Java文件的内容
         */
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    /**
     * 用于构造JavaFileObject，传输Class文件
     */
    public static class JavaClassObject extends SimpleJavaFileObject {

        protected final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        protected JavaClassObject(String name, Kind kind) {
            super(URI.create("string:///" + name.replace(".", "/") + kind.extension), kind);
        }

        public byte[] getBytes() {
            return byteArrayOutputStream.toByteArray();
        }

        @Override
        public OutputStream openOutputStream() {
            return byteArrayOutputStream;
        }
    }

    /**
     * 将compile的结果重定向到一个JavaClassObject中
     */
    private static class ClassFileManager extends ForwardingJavaFileManager<JavaFileManager> {

        private JavaClassObject javaClassObject;

        protected ClassFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        /**
         * compile时会调用该方法获取输出流对应的JavaClassObject
         */
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            javaClassObject = new JavaClassObject(className, kind);
            return javaClassObject;
        }

        /**
         * 暴露JavaClassObject，从而可以在代码中获取class文件对应的字节码
         */
        public byte[] getBytes() {
            if (javaClassObject == null) {
                return null;
            }
            return javaClassObject.getBytes();
        }
    }

}

