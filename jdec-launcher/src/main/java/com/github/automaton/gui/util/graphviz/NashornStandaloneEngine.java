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

import javax.script.*;

import guru.nidi.graphviz.engine.*;

/**
 * Provides an implementation of {@link JavascriptEngine} that uses the
 * standalone <a href="https://github.com/openjdk/nashorn">Nashorn Engine</a>.
 * 
 * @author Sung Ho Yoon
 * @since 1.3
 */
public class NashornStandaloneEngine extends AbstractJavascriptEngine {
    private static final ScriptEngine ENGINE = new ScriptEngineManager().getEngineByName("nashorn");
    private final ScriptContext context = new SimpleScriptContext();
    private final ResultHandler resultHandler = new ResultHandler();

    /**
     * Constructs a new {@code NashornStandaloneEngine}.
     * 
     * @throws MissingDependencyException if the Nashorn engine is not available
     */
    public NashornStandaloneEngine() {
        if (ENGINE == null) {
            throw new MissingDependencyException("Nashorn engine is not available", "org.openjdk.nashorn:nashorn-core");
        }
        context.getBindings(ScriptContext.ENGINE_SCOPE).put("handler", resultHandler);
        eval("function result(r){ handler.setResult(r); }"
                + "function error(r){ handler.setError(r); }"
                + "function log(r){ handler.log(r); }");
    }

    /**
     * Executes the specified Javascript snippet.
     * 
     * @param js a Javascript snippet
     * @throws GraphvizException if there is a problem executing the argument
     */
    @Override
    protected String execute(String js) {
        eval(js);
        return resultHandler.waitFor();
    }

    private void eval(String js) {
        try {
            ENGINE.eval(js, context);
        } catch (ScriptException e) {
            throw new GraphvizException("Problem executing javascript", e);
        }
    }
}
