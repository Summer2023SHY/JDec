package com.github.automaton.io.json;

import java.util.*;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.gson.*;
import com.google.gson.reflect.*;

/**
 * Provides utility methods for interaction with {@link com.google.gson gson}.
 * 
 * @author Sung Ho Yoon
 * @since 2.0
 */
public class JsonUtils {

    /** Internally used gson instance */
    private static Gson gson = new Gson();

    /** Private constructor */
    private JsonUtils() {
    }

    /**
     * Adds a parameterized {@link java.util.List List} to a JSON object as a property.
     * 
     * @param <T> type of data stored in the list
     * @param jsonObj the JSON object to add property to
     * @param name name of the added property
     * @param list a list of data to store in the JSON object
     * @param type the type of data stored in the specified list
     */
    public static <T> void addListPropertyToJsonObject(JsonObject jsonObj, String name, List<T> list, Class<T> type) {
        jsonObj.add(
                name,
                gson.toJsonTree(
                        list,
                        TypeUtils.parameterize(list.getClass(), type)));
    }

    /**
     * Loads a property from a JSON object as a parameterized {@link java.util.List List}.
     * 
     * @param <T> type of data stored in the property
     * @param jsonObj the JSON object to load property from
     * @param name name of the property
     * @param type the type of data stored in the property
     * @return the data stored in the specified property as a list
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> readListPropertyFromJsonObject(JsonObject jsonObj, String name, Class<T> type) {
        return gson.fromJson(
                jsonObj.get(name),
                (TypeToken<ArrayList<T>>) TypeToken.getParameterized(ArrayList.class, type));
    }
}
