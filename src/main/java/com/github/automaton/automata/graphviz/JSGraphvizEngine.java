package com.github.automaton.automata.graphviz;

import guru.nidi.graphviz.engine.*;

/**
 * Implementation of {@link GraphvizEngine} that uses
 * {@link NashornStandaloneEngine}.
 * 
 * @author Sung Ho Yoon
 * @since 1.3
 */
public class JSGraphvizEngine extends AbstractJsGraphvizEngine {

    /**
     * Constructs a new {@code NashornGraphvizEngine}.
     */
    public JSGraphvizEngine() {
        super(false, () -> new NashornStandaloneEngine());
    }

    @Override
    protected void doInit() {
        final JavascriptEngine engine = engine();
        engine.executeJavascript(promiseJsCode());
        super.doInit();
    }
}
