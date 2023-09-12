package com.github.automaton.io.json;

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
