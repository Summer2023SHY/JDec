package com.github.automaton.io;

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

    /**
     * Checks whether there is enough space available on the file system.
     * 
     * @param nBytes size of the file to create, in bytes
     * 
     * @throws IOException if there is not enough space to write on the file system
     * 
     * @see File#getUsableSpace()
     * 
     * @since 2.0
     */
    public static void checkSpace(long nBytes) throws IOException {
        if (nBytes > getTemporaryFile().getUsableSpace()) {
            throw new IOException("There is not enough space to write file with size " + nBytes +".");
        }
    }
}
