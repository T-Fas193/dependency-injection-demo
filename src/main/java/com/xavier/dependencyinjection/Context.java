package com.xavier.dependencyinjection;

import java.lang.reflect.InvocationTargetException;
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

    public <T, I extends T> void bind(Class<T> typeClass, Class<I> implementClass) {
        try {
            I instance = implementClass.getConstructor().newInstance();
            components.put(typeClass, instance);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
