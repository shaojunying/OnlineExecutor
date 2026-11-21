package io.github.shaojunying.onlineexecutor.compile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;

import org.springframework.stereotype.Component;

/**
 * Compiles one Java source file in memory. No file is written to disk.
 *
 * <p>The compiler first parses the source to find the public top-level class,
 * because javac requires the file name to match it. Then it compiles the source
 * and keeps every class file that javac produces, including nested and anonymous classes.
 *
 * <p>User code compiles against the JDK only. The server class path is not visible.
 *
 * <p>References:
 * <a href="https://stackoverflow.com/questions/12173294">Compile code fully in memory with javax.tools.JavaCompiler</a>
 */
@Component
public class InMemoryJavaCompiler {

    /** File name used when the source has no public top-level class. */
    static final String DEFAULT_CLASS_NAME = "Main";

    private static final List<String> OPTIONS = List.of("-proc:none", "-g", "-Xlint:none");

    private final JavaCompiler compiler;

    public InMemoryJavaCompiler() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "No system Java compiler found. Run the server on a JDK, not on a JRE.");
        }
    }

    public CompilationResult compile(String source) {
        SourceLayout layout = analyze(source);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            standard.setLocation(StandardLocation.CLASS_PATH, List.of());
            InMemoryFileManager fileManager = new InMemoryFileManager(standard);
            JavaFileObject file = new SourceFile(layout.fileName(), source);

            boolean ok = compiler.getTask(null, fileManager, diagnostics, OPTIONS, null, List.of(file)).call();
            if (!ok) {
                return new CompilationResult.Failure(errorsOf(diagnostics));
            }
            return new CompilationResult.Success(
                    new CompiledProgram(layout.mainClassName(), fileManager.classes()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Parses the source to find the file name javac requires and the class to start.
     * Parse errors are ignored here. The compile step reports them.
     */
    SourceLayout analyze(String source) {
        JavaFileObject probe = new SourceFile(DEFAULT_CLASS_NAME, source);
        JavacTask task = (JavacTask) compiler.getTask(null, null, diagnostic -> { }, OPTIONS, null, List.of(probe));
        String publicClass = null;
        String classWithMain = null;
        String packagePrefix = "";
        try {
            for (CompilationUnitTree unit : task.parse()) {
                if (unit.getPackageName() != null) {
                    packagePrefix = unit.getPackageName() + ".";
                }
                for (Tree declaration : unit.getTypeDecls()) {
                    if (!(declaration instanceof ClassTree type)) {
                        continue;
                    }
                    String name = type.getSimpleName().toString();
                    Set<Modifier> modifiers = type.getModifiers().getFlags();
                    if (publicClass == null && modifiers.contains(Modifier.PUBLIC)) {
                        publicClass = name;
                    }
                    if (classWithMain == null && declaresMain(type)) {
                        classWithMain = name;
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String fileName = publicClass != null ? publicClass : DEFAULT_CLASS_NAME;
        String mainClass = publicClass != null ? publicClass
                : classWithMain != null ? classWithMain
                : DEFAULT_CLASS_NAME;
        return new SourceLayout(fileName, packagePrefix + mainClass);
    }

    private static boolean declaresMain(ClassTree type) {
        return type.getMembers().stream()
                .anyMatch(member -> member instanceof MethodTree method
                        && method.getName().contentEquals("main"));
    }

    private static List<CompileError> errorsOf(DiagnosticCollector<JavaFileObject> diagnostics) {
        List<CompileError> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                long line = Math.max(d.getLineNumber(), 0);
                long column = Math.max(d.getColumnNumber(), 0);
                errors.add(new CompileError(line, column, d.getMessage(Locale.ROOT)));
            }
        }
        return errors;
    }

    record SourceLayout(String fileName, String mainClassName) {
    }

    /** Source code held in a string. */
    private static final class SourceFile extends SimpleJavaFileObject {

        private final String code;

        SourceFile(String className, String code) {
            super(URI.create("string:///" + className + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    /** A class file that javac writes to memory. */
    private static final class ClassFile extends SimpleJavaFileObject {

        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        ClassFile(String binaryName) {
            super(URI.create("bytes:///" + binaryName.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public OutputStream openOutputStream() {
            return bytes;
        }

        byte[] bytes() {
            return bytes.toByteArray();
        }
    }

    /** Sends every class file javac writes to memory. */
    private static final class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {

        private final Map<String, ClassFile> outputs = new LinkedHashMap<>();

        InMemoryFileManager(JavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            ClassFile file = new ClassFile(className);
            outputs.put(className, file);
            return file;
        }

        Map<String, byte[]> classes() {
            Map<String, byte[]> result = new LinkedHashMap<>();
            outputs.forEach((name, file) -> result.put(name, file.bytes()));
            return result;
        }
    }
}
