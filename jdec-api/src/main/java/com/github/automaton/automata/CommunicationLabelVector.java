/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

import java.util.*;

import org.apache.commons.lang3.*;

/**
 * Represents both a vector label and its associated communication roles (which
 * implies that we are only using this for event labels, not state labels).
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
class CommunicationLabelVector extends LabelVector {

    /* INSTANCE VARIABLE */

    public CommunicationRole[] roles;

    /* CONSTRUCTOR */

    /**
     * Private constructor for compatibility with gson
     * 
     * @since 2.0
     */
    private CommunicationLabelVector() {
        super(StringUtils.EMPTY);
        this.roles = (CommunicationRole[]) ArrayUtils.EMPTY_OBJECT_ARRAY;
    }

    /**
     * Construct a CommunicationLabelVector object, given it's label and each
     * controller's communication roles.
     * 
     * @param label The unvectorized label
     * @param roles The array of communication roles
     **/
    public CommunicationLabelVector(String label, CommunicationRole[] roles) {

        super(label);
        this.roles = roles;

    }

    /* OVERRIDDEN METHOD */

    /**
     * Check for equality by comparing labels and roles.
     * 
     * @param other The communication label vector to compare this one to
     * @return Whether or not the communication label vectors are equal
     **/
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        else if (!super.equals(other))
            return false;
        else if (other instanceof CommunicationLabelVector)
            return Arrays.deepEquals(roles, ((CommunicationLabelVector) other).roles);
        else
            return false;
    }

}
