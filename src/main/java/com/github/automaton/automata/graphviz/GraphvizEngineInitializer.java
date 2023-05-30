package com.github.automaton.automata.graphviz;

import java.lang.reflect.*;
import java.util.*;
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
    public static boolean setupGraphvizEngines() {
        List<GraphvizEngine> engines = new ArrayList<>();
        try {
            boolean cmdLineEngineAvailable = (boolean) FieldUtils.readStaticField(
                    GraphvizCmdLineEngine.class, "AVAILABLE", true);
            if (cmdLineEngineAvailable) {
                engines.add(new GraphvizCmdLineEngine());
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
