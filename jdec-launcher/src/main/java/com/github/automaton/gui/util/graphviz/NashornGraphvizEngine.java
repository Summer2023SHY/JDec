/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util.graphviz;

import guru.nidi.graphviz.engine.*;

/**
 * Implementation of {@link GraphvizEngine} that uses
 * {@link NashornStandaloneEngine}.
 * 
 * @author Sung Ho Yoon
 * @since 1.3
 */
public class NashornGraphvizEngine extends AbstractJsGraphvizEngine {

    /**
     * Constructs a new {@code NashornGraphvizEngine}.
     */
    public NashornGraphvizEngine() {
        super(false, () -> new NashornStandaloneEngine());
    }

    @Override
    protected void doInit() {
        final JavascriptEngine engine = engine();
        engine.executeJavascript(promiseJsCode());
        super.doInit();
    }
}
