package com.github.automaton.io;

import java.io.*;
import java.nio.file.*;

/**
 * This class contains various methods for assisting
 * I/O operations with automata.
 * 
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * @since 1.1
 */
public class IOUtility {

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
                System.out.println("WARNING: Temporary file had to be manually created.");

                if (!file.exists()) {

                    try {
                        if (!file.createNewFile())
                            System.err.println("ERROR: Could not create empty temporary file.");
                    } catch (IOException e2) {
                        System.err.println("ERROR: Could not create empty temporary file.");
                        e2.printStackTrace();
                    }

                    file.deleteOnExit();
                    return file;
                }

            } // while

        } // catch

    }
}
