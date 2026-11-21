package io.github.shaojunying.onlineexecutor.compile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryJavaCompilerTest {

    private final InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();

    @Test
    void compilesPublicClass() {
        CompiledProgram program = compileOk("""
                public class Test {
                    public static void main(String[] args) {
                        System.out.println("Hello");
                    }
                }
                """);

        assertThat(program.mainClassName()).isEqualTo("Test");
        assertThat(program.classes()).containsOnlyKeys("Test");
    }

    /** The old code took everything up to the next space as the class name: "C{". */
    @Test
    void acceptsBraceDirectlyAfterClassName() {
        CompiledProgram program = compileOk("public class C{ public static void main(String[] a) {} }");

        assertThat(program.mainClassName()).isEqualTo("C");
    }

    @Test
    void ignoresClassKeywordInCommentsAndStrings() {
        CompiledProgram program = compileOk("""
                // This class prints a word.
                public class Real {
                    static String text = "a class of its own";
                    public static void main(String[] args) {}
                }
                """);

        assertThat(program.mainClassName()).isEqualTo("Real");
    }

    /** The old code kept only the last class file that javac wrote. */
    @Test
    void keepsNestedAndAnonymousClasses() {
        CompiledProgram program = compileOk("""
                public class Outer {
                    static class Inner {}
                    public static void main(String[] args) {
                        Runnable r = new Runnable() { public void run() {} };
                    }
                }
                """);

        assertThat(program.classes()).containsOnlyKeys("Outer", "Outer$Inner", "Outer$1");
    }

    @Test
    void usesPackageInMainClassName() {
        CompiledProgram program = compileOk("""
                package demo.app;
                public class App { public static void main(String[] args) {} }
                """);

        assertThat(program.mainClassName()).isEqualTo("demo.app.App");
        assertThat(program.classes()).containsOnlyKeys("demo.app.App");
    }

    @Test
    void picksClassWithMainWhenNoClassIsPublic() {
        CompiledProgram program = compileOk("""
                class Helper {}
                class Program { public static void main(String[] args) {} }
                """);

        assertThat(program.mainClassName()).isEqualTo("Program");
    }

    @Test
    void reportsCompileErrorsWithPosition() {
        CompilationResult result = compiler.compile("""
                public class Broken {
                    public static void main(String[] args) {
                        List<Integer> list = null;
                    }
                }
                """);

        assertThat(result).isInstanceOf(CompilationResult.Failure.class);
        CompileError error = ((CompilationResult.Failure) result).errors().getFirst();
        assertThat(error.line()).isEqualTo(3);
        assertThat(error.column()).isEqualTo(9);
        assertThat(error.message()).contains("cannot find symbol");
    }

    @Test
    void reportsSourceWithoutClassAsCompileError() {
        CompilationResult result = compiler.compile("System.out.println(1);");

        assertThat(result).isInstanceOf(CompilationResult.Failure.class);
    }

    @Test
    void doesNotExposeServerClassPath() {
        CompilationResult result = compiler.compile("""
                import org.springframework.boot.SpringApplication;
                public class Peek { public static void main(String[] args) {} }
                """);

        assertThat(result).isInstanceOf(CompilationResult.Failure.class);
        assertThat(((CompilationResult.Failure) result).errors().getFirst().message()).contains("does not exist");
    }

    private CompiledProgram compileOk(String source) {
        CompilationResult result = compiler.compile(source);
        assertThat(result).isInstanceOf(CompilationResult.Success.class);
        return ((CompilationResult.Success) result).program();
    }
}
