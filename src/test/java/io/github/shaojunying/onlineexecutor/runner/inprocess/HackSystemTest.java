package io.github.shaojunying.onlineexecutor.runner.inprocess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class HackSystemTest {

    /**
     * User code calls HackSystem wherever it called System. A missing method would fail
     * at run time with NoSuchMethodError, so HackSystem must mirror System exactly.
     */
    @Test
    void declaresEveryPublicStaticMemberOfSystem() throws Exception {
        for (Method method : System.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getDeclaringClass() != System.class) {
                continue;
            }
            Method copy = HackSystem.class.getMethod(method.getName(), method.getParameterTypes());
            assertThat(copy.getReturnType()).as(method.toString()).isEqualTo(method.getReturnType());
            assertThat(Modifier.isStatic(copy.getModifiers())).as(method.toString()).isTrue();
        }
        for (Field field : System.class.getFields()) {
            Field copy = HackSystem.class.getField(field.getName());
            assertThat(copy.getType()).as(field.toString()).isEqualTo(field.getType());
        }
    }

    @Test
    void sendsOutputToContextOfCurrentThread() throws Exception {
        ExecutionContext first = new ExecutionContext("", 1000);
        ExecutionContext second = new ExecutionContext("", 1000);

        Thread a = new Thread(() -> printIn(first, "a"));
        Thread b = new Thread(() -> printIn(second, "b"));
        a.start();
        b.start();
        a.join();
        b.join();

        assertThat(first.stdout.content()).isEqualTo("a".repeat(100));
        assertThat(second.stdout.content()).isEqualTo("b".repeat(100));
    }

    @Test
    void threadsStartedByUserCodeInheritContext() throws Exception {
        ExecutionContext context = new ExecutionContext("", 1000);
        context.bindToCurrentThread();
        try {
            Thread child = new Thread(() -> HackSystem.out.print("child"));
            child.start();
            child.join();
        } finally {
            ExecutionContext.unbindCurrentThread();
        }

        assertThat(context.stdout.content()).isEqualTo("child");
    }

    @Test
    void readsStdinOfContext() throws Exception {
        ExecutionContext context = new ExecutionContext("input", 1000);
        context.bindToCurrentThread();
        try {
            assertThat(new String(HackSystem.in.readAllBytes())).isEqualTo("input");
        } finally {
            ExecutionContext.unbindCurrentThread();
        }
    }

    @Test
    void exitThrowsInsteadOfStoppingJvm() {
        assertThatThrownBy(() -> HackSystem.exit(2))
                .isInstanceOfSatisfying(UserCodeStop.class, stop -> {
                    assertThat(stop.reason()).isEqualTo(UserCodeStop.Reason.EXIT);
                    assertThat(stop.exitCode()).isEqualTo(2);
                });
    }

    @Test
    void writeAfterCloseStopsUserCode() {
        ExecutionContext context = new ExecutionContext("", 1000);
        context.bindToCurrentThread();
        try {
            context.close();
            assertThatThrownBy(() -> HackSystem.out.print("late"))
                    .isInstanceOfSatisfying(UserCodeStop.class,
                            stop -> assertThat(stop.reason()).isEqualTo(UserCodeStop.Reason.CANCELLED));
        } finally {
            ExecutionContext.unbindCurrentThread();
        }
    }

    @Test
    void getPropertiesReturnsCopy() {
        HackSystem.getProperties().setProperty("online.executor.test", "x");

        assertThat(System.getProperty("online.executor.test")).isNull();
    }

    private static void printIn(ExecutionContext context, String text) {
        context.bindToCurrentThread();
        try {
            List<String> parts = Arrays.asList(text.repeat(100).split(""));
            parts.forEach(HackSystem.out::print);
        } finally {
            ExecutionContext.unbindCurrentThread();
        }
    }
}
