package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
            Optional<Constructor<?>> optionalConstructor = Arrays.stream(implementClass.getConstructors()).filter(constructor -> constructor.isAnnotationPresent(Inject.class)).findFirst();
            if (optionalConstructor.isPresent()) {
                Object instance = optionalConstructor.get().newInstance(components.values().toArray());
                components.put(typeClass, instance);
                return;
            }
            I instance = implementClass.getConstructor().newInstance();
            components.put(typeClass, instance);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
