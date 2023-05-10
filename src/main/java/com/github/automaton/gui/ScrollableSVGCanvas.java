package com.github.automaton.gui;

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
 * @since 2.0
 */
class ScrollableSVGCanvas extends JSVGCanvas implements MouseWheelListener {

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
            public boolean startInteraction(InputEvent ie) {
                int mods = ie.getModifiersEx();
                return ie.getID() == MouseEvent.MOUSE_PRESSED &&
                        (mods & InputEvent.BUTTON1_DOWN_MASK) != 0;
            }
        };
        intl.add(super.panInteractor);
        addMouseWheelListener(this);
        setEnableZoomInteractor(false);
        setEnableRotateInteractor(false);
        setEnablePanInteractor(true);
    }

    /**
     * Implements scrolling / zooming with scroll wheel.
     * {@inheritDoc}
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isControlDown()) {
            if (e.getWheelRotation() < 0) {
                JComponent component = (JComponent) e.getComponent();
                Action action = component.getActionMap().get(ZOOM_IN_ACTION);
                if (action != null)
                    action.actionPerformed(null);

            } else if (e.getWheelRotation() > 0) {
                JComponent component = (JComponent) e.getComponent();
                Action action = component.getActionMap().get(ZOOM_OUT_ACTION);
                if (action != null)
                    action.actionPerformed(null);

            }
        } else {
            if (e.getWheelRotation() < 0) {
                JComponent component = (JComponent) e.getComponent();
                Action action = component.getActionMap().get(FAST_SCROLL_UP_ACTION);
                if (action != null)
                    action.actionPerformed(null);

            } else if (e.getWheelRotation() > 0) {
                JComponent component = (JComponent) e.getComponent();
                Action action = component.getActionMap().get(FAST_SCROLL_DOWN_ACTION);
                if (action != null)
                    action.actionPerformed(null);

            } else {
                System.out.println("scrolled down");
            }
        }
    }

}
