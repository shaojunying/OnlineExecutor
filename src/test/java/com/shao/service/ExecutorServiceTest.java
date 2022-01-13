package com.shao.service;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class ExecutorServiceTest {

    @Test
    void executor() {
        String classContent =
                "public class Test { " +
                "   public static void main(String[] args) { " +
                "       List<Integer> list = Arrays.asList(1, 2, 3);" +
                "       System.out.println(\"Hello World!!!hhh\"); " +
                "   }" +
                "}";
        ExecutorService service = new ExecutorService();
        String output = service.executor(classContent);
    }
}
