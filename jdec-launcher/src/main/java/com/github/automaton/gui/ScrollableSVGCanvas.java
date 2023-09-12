package com.github.automaton.gui;

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

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.*;
import org.apache.batik.swing.svg.SVGUserAgent;

/**
 * {@link JSVGCanvas} with "friendlier" mouse interaction support.
 * 
 * @author Sung Ho Yoon
 * @since 1.3
 */
class ScrollableSVGCanvas extends JSVGCanvas {

    /**
     * Creates a new {@code ScrollableSVGCanvas}.
     */
    public ScrollableSVGCanvas() {
        this(null, false, false);
    }

    /**
     * Creates a new {@code ScrollableSVGCanvas}.
     * 
     * @param ua             a {@link SVGUserAgent} instance or {@code null}.
     * @param eventsEnabled  Whether the underlying GVT tree should be reactive
     *                       to mouse and key events.
     * @param selectableText Whether the text in the loaded SVG document
     *                       should be selectable.
     * 
     * @see JSVGCanvas#JSVGCanvas(SVGUserAgent, boolean, boolean)
     */
    @SuppressWarnings("unchecked")
    public ScrollableSVGCanvas(SVGUserAgent ua,
            boolean eventsEnabled,
            boolean selectableText) {
        super(ua, eventsEnabled, selectableText);
        List<Interactor> intl = getInteractors();
        intl.remove(super.panInteractor);
        super.panInteractor = new AbstractPanInteractor() {
            @Override
            public boolean startInteraction(InputEvent ie) {
                int mods = ie.getModifiersEx();
                return ie.getID() == MouseEvent.MOUSE_PRESSED &&
                        (mods & InputEvent.BUTTON1_DOWN_MASK) != 0;
            }
        };
        intl.add(super.panInteractor);
        addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                if (e.getWheelRotation() < 0) {
                    Action action = getActionMap().get(ZOOM_IN_ACTION);
                    if (action != null)
                        action.actionPerformed(null);

                } else if (e.getWheelRotation() > 0) {
                    Action action = getActionMap().get(ZOOM_OUT_ACTION);
                    if (action != null)
                        action.actionPerformed(null);

                }
            } else {
                if (e.getWheelRotation() < 0) {
                    Action action = getActionMap().get(FAST_SCROLL_UP_ACTION);
                    if (action != null)
                        action.actionPerformed(null);

                } else if (e.getWheelRotation() > 0) {
                    Action action = getActionMap().get(FAST_SCROLL_DOWN_ACTION);
                    if (action != null)
                        action.actionPerformed(null);

                }
            }
        });
        setEnableZoomInteractor(false);
        setEnableRotateInteractor(false);
        setEnablePanInteractor(true);
    }

}
