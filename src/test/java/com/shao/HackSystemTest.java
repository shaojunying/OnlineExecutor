package com.shao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HackSystemTest {

    @Test
    public void testPrintln() {
        HackSystem.out.println(111);
        assertEquals("111\n", HackSystem.getBufferString());
        HackSystem.clearBuffer();
        assertEquals(0, HackSystem.getBufferString().length());
        HackSystem.out.println("Hello World");
        assertEquals("Hello World\n", HackSystem.getBufferString());
    }

}
