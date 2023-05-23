package com.github.automaton.automata.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

@DisplayName("BYTE MANIPULATOR")
public class ByteManipulatorTest {

    @Test
    @DisplayName("Ensuring that values being written can be read again")
    public void byteManipulatorTest1() {
        for (int i = 0; i <= 255; i++) {
            byte[] arr = new byte[1];
            ByteManipulator.writeLongAsBytes(arr, 0, i, 1);
            assertEquals(i, ByteManipulator.readBytesAsLong(arr, 0, 1));
        }
    }

    @Test
    @DisplayName("Ensuring that one byte values were written and read properly")
    public void byteManipulatorTest2() {
        for (int i = 256; i <= 65535; i++) {
            byte[] arr = new byte[2];
            ByteManipulator.writeLongAsBytes(arr, 0, i, 2);
            assertEquals(i, ByteManipulator.readBytesAsLong(arr, 0, 2));
        }
    }
}
