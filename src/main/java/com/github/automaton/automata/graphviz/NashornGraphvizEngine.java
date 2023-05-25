package com.github.automaton.automata.graphviz;

import guru.nidi.graphviz.engine.*;

/**
 * Implementation of {@link GraphvizEngine} that uses
 * {@link NashornStandaloneEngine}.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
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
