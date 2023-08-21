package com.github.automaton.io;

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

import java.io.*;
import java.nio.file.*;

import org.apache.logging.log4j.*;

/**
 * This class contains various methods for assisting
 * I/O operations with automata.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * @since 1.1
 */
public class IOUtility {

    private static Logger logger = LogManager.getLogger();

    /* CLASS VARIABLES */
    private static int temporaryFileIndex = 1;

    /** Private constructor */
    private IOUtility() {}

    /**
     * Get an unused temporary file.
     * 
     * @implNote These temporary files do not have extensions. Do not use them
     *           directly in JDec.
     * @return The temporary file
     **/
    public static File getTemporaryFile() {

        try {

            File file = Files.createTempFile(null, null).toFile();
            file.deleteOnExit();
            return file;

        } catch (Exception e1) {

            // Continue to try getting a temporary file until we've found one that hasn't
            // been used
            while (true) {

                File file = new File(".tmp" + temporaryFileIndex++);
                logger.warn("Temporary file had to be manually created.");

                if (!file.exists()) {

                    try {
                        if (!file.createNewFile())
                            logger.error("Could not create empty temporary file.");
                    } catch (IOException e2) {
                        logger.error("Could not create empty temporary file.", e2);
                    }

                    file.deleteOnExit();
                    return file;
                }

            } // while

        } // catch

    }
}
