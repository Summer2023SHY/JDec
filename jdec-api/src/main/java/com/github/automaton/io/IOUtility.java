/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

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
 * 
 * @deprecated This class is no longer used by JDec, and is subject to removal.
 */
@Deprecated(since = "2.0", forRemoval = true)
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
     * 
     * @deprecated This method is no longer used by JDec.
     **/
    @Deprecated(since = "2.0", forRemoval = true)
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
