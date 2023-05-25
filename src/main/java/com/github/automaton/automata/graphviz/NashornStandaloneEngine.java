package com.github.automaton.automata.graphviz;

import javax.script.*;

import guru.nidi.graphviz.engine.*;

/**
 * Provides an implementation of {@link JavascriptEngine} that uses the
 * standalone <a href="https://github.com/openjdk/nashorn">Nashorn Engine</a>.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
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
