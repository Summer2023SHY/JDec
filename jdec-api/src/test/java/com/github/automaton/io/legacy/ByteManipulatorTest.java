package com.github.automaton.io.legacy;

/* 
 * Copyright (C) 2016 Micah Stairs
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
