package com.github.automaton.io.json;

import java.util.*;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.gson.*;
import com.google.gson.reflect.*;

public class JsonUtils {

    private static Gson gson = new Gson();

    private JsonUtils() {
    }

    public static <T> void addListPropertyToJsonObject(JsonObject jsonObj, String name, List<T> list, Class<T> type) {
        jsonObj.add(
                name,
                gson.toJsonTree(
                        list,
                        TypeUtils.parameterize(list.getClass(), type)));
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> readListPropertyFromJsonObject(JsonObject jsonObj, String name, Class<T> type) {
        return gson.fromJson(
                jsonObj.get(name),
                (TypeToken<ArrayList<T>>) TypeToken.getParameterized(ArrayList.class, type));
    }
}
