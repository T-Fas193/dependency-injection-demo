package com.xavier.dependencyinjection;

import java.util.HashMap;
import java.util.Map;

public class Context {

    Map<Class<?>, Object> components = new HashMap<>();

    public <T> T get(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }

    public <T> void bind(Class<T> componentClass, T component) {
        components.put(componentClass, component);
    }
}
