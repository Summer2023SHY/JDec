/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.apache.logging.log4j.*;
import org.junit.jupiter.api.*;

public class IDUtilTest {

    private static Logger logger = LogManager.getLogger();

    @Test
    @DisplayName("createCombinedIDWithOrderedSet() Tests")
    public void testCreateCombinedIDWithOrderedSet() {
        /* createCombinedIDWithOrderedSet() Tests */

        logger.debug("Combining IDs - combineIDs()");

        List<Long> list = List.of(4L, 2L, 7L);
        logger.debug("Ensuring that {4,2,7} with a max ID of 7 maps to 279");
        assertEquals(279, IDUtil.combineIDs(list, 7));

        logger.debug("Separating IDs - separateIDs()");
        logger.debug("Ensuring that 279 with a max ID of 7 maps back to {4,2,7}");
        assertIterableEquals(list, IDUtil.separateIDs(279, 7));
    }
}
