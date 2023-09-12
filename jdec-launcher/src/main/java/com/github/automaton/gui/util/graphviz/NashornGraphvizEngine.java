package com.github.automaton.gui.util.graphviz;

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
