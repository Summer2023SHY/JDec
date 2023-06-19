package com.github.automaton.automata.util;

import java.util.Objects;

/* 
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

import org.jgrapht.graph.DefaultEdge;

/**
 * A type of undirected / unweighted edge that has a label associated with it.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public class LabeledEdge<T> extends DefaultEdge {

    /** The label associated with this edge */
    private T label;

    /** Default constructor. */
    public LabeledEdge() {
    }

    /**
     * Constructs a new {@code LabeledEdge} with the specified label.
     * 
     * @param label a label
     */
    public LabeledEdge(T label) {
        this.label = label;
    }

    /**
     * Returns the label associated with this edge.
     * 
     * @return a label
     */
    public T getLabel() {
        return label;
    }

    /**
     * Returns the source of this labeled edge.
     * 
     * @return the source of this edge
     */
    @Override
    public final Object getSource() {
        return super.getSource();
    }

    /**
     * Returns the target of this labeled edge.
     * 
     * @return the target of this edge
     */
    @Override
    public final Object getTarget() {
        return super.getTarget();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.getSource(), super.getTarget(), label);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof LabeledEdge) {
            LabeledEdge<?> other = (LabeledEdge<?>) obj;
            return Objects.equals(super.getSource(), other.getSource())
                && Objects.equals(super.getTarget(), other.getTarget())
                && Objects.equals(this.getLabel(), other.getLabel());
        } else
            return false;
    }

    @Override
    public String toString() {
        return String.format(
            "(%s - %s) : %s",
            super.getSource().toString(), super.getTarget().toString(), label.toString()
        );
    }

}
