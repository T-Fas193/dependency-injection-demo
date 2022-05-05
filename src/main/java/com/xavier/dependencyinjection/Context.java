package com.xavier.dependencyinjection;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

public class Context {

    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <T> T get(Class<T> componentClass) {
        return (T) providers.get(componentClass).get();
    }

    public <T, I extends T> void bind(Class<T> typeClass, I implementationInstance) {
        providers.put(typeClass, () -> implementationInstance);
    }

    public <T, I extends T> void bind(Class<T> typeClass, Class<I> implementationClass) {
        try {
            Constructor<?>[] injectionConstructors = stream(implementationClass.getConstructors()).filter(constructor -> constructor.isAnnotationPresent(Inject.class)).toArray(Constructor<?>[]::new);
            if (injectionConstructors.length > 1) throw new MultipleInjectionFoundException();

            if (injectionConstructors.length > 0) {
                bindInjectionConstructorInstance(typeClass, injectionConstructors[0]);
                return;
            }
            bindDefaultConstructorInstance(typeClass, implementationClass);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private <T> void bindInjectionConstructorInstance(Class<T> typeClass, Constructor<?> constructor) throws ReflectiveOperationException {
        Object[] constructorParameters = stream(constructor.getParameterTypes())
                .map(parameterType -> ofNullable(providers.get(parameterType)).orElseThrow(DependencyNotExists::new).get())
                .toArray();
        Object instance = constructor.newInstance(constructorParameters);
        providers.put(typeClass, () -> instance);
    }

    private <T, I extends T> void bindDefaultConstructorInstance(Class<T> typeClass, Class<I> implementClass) throws ReflectiveOperationException {
        I instance = implementClass.getConstructor().newInstance();
        providers.put(typeClass, () -> instance);
    }
}
