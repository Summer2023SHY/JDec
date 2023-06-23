package com.github.automaton.automata.graphviz;

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

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.lang3.reflect.*;
import org.apache.logging.log4j.*;

import guru.nidi.graphviz.engine.*;

/**
 * Sets up Graphviz engines, including the builtin engines and
 * the {@link JSGraphvizEngine Nashorn standalone engine}.
 * 
 * @see Graphviz#useEngine(GraphvizEngine, GraphvizEngine...)
 * 
 * @author Sung Ho Yoon
 * 
 * @since 1.3
 */
public class GraphvizEngineInitializer {

    private static Logger logger = LogManager.getLogger();

    private GraphvizEngineInitializer() {
    }

    /**
     * Attempts to set up {@link GraphvizCmdLineEngine CMD line engine}, Graal-based
     * engine, and
     * {@link NashornGraphvizEngine Nashorn-based engine}.
     * 
     * @return {@code true} if there is at least one engine available for use
     */
    @SuppressWarnings("resource")
    public static boolean setupGraphvizEngines() {
        List<GraphvizEngine> engines = new ArrayList<>();
        try {
            boolean cmdLineEngineAvailable = (boolean) FieldUtils.readStaticField(
                    GraphvizCmdLineEngine.class, "AVAILABLE", true);
            if (cmdLineEngineAvailable) {
                engines.add(new GraphvizCmdLineEngine().timeout(10, TimeUnit.MINUTES));
            }
        } catch (ReflectiveOperationException ref) {
            logger.info("CMD line engine is not available", ref);
        }
        try {
            Method graalEngineGetter = MethodUtils.getMatchingMethod(GraphvizJdkEngine.class, "tryGraal");
            graalEngineGetter.setAccessible(true);
            if (Objects.nonNull(graalEngineGetter.invoke(null))) {
                Supplier<JavascriptEngine> graalEngineSupplier = () -> {
                    try {
                        return (JavascriptEngine) graalEngineGetter.invoke(null);
                    } catch (ReflectiveOperationException e) {
                        return null;
                    }
                };
                engines.add(new AbstractJsGraphvizEngine(false, graalEngineSupplier) {
                });
            } else {
                logger.info("Graal engine is not available");
            }
        } catch (Exception e) {
            logger.info("Graal engine is not available", e);
        }
        try {
            engines.add(new NashornGraphvizEngine());
        } catch (MissingDependencyException mis) {
            logger.info("Nashorn engine is not available", mis);
        }
        if (engines.isEmpty()) {
            logger.error("No Graphviz engine available. Diagram will not be generated.");
            return false;
        }
        Graphviz.useEngine(engines);
        return true;
    }
}
