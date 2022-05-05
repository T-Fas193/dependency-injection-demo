package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.Optional.*;

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
            long injectionConstructorCount = stream(implementClass.getConstructors()).filter(constructor -> constructor.isAnnotationPresent(Inject.class)).count();
            if (injectionConstructorCount > 1) throw new UnsupportedOperationException();

            Optional<Constructor<?>> optionalConstructor = stream(implementClass.getConstructors())
                    .filter(constructor -> constructor.isAnnotationPresent(Inject.class)).findFirst();
            if (optionalConstructor.isPresent()) {
                Constructor<?> constructor = optionalConstructor.get();
                Object[] constructorParameters = stream(constructor.getParameterTypes())
                        .map(parameterType -> ofNullable(components.get(parameterType))
                                .orElseThrow(DependencyNotExists::new))
                        .toArray();
                Object instance = constructor.newInstance(constructorParameters);
                components.put(typeClass, instance);
                return;
            }
            I instance = implementClass.getConstructor().newInstance();
            components.put(typeClass, instance);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new UnsupportedOperationException(e);
        }
    }
}
