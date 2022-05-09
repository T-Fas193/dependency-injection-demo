package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.stream;

class DefaultComponentProvider<T> implements ContextConfig.ComponentProvider<T> {

    private final ContextConfig contextConfig;
    private final Constructor<T> constructor;

    DefaultComponentProvider(ContextConfig contextConfig, Class<T> implementationClass) {
        this.contextConfig = contextConfig;
        this.constructor = (Constructor<T>) getInjectionConstructor(implementationClass);
    }

    private Constructor<?> getInjectionConstructor(Class<?> implementationClass) {
        int modifiers = implementationClass.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers))
            throw new UnsupportedOperationException();

        Constructor<?>[] injectionConstructors = stream(implementationClass.getConstructors())
                .filter(implementationConstructor -> implementationConstructor.isAnnotationPresent(Inject.class))
                .toArray(Constructor<?>[]::new);
        if (injectionConstructors.length > 1) throw new MultipleInjectionFoundException();

        return stream(injectionConstructors).findFirst().orElseGet(() -> {
            try {
                return implementationClass.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException(e);
            }
        });
    }

    @Override
    public T get() {
        try {
            Object[] constructorParameters = stream(constructor.getParameterTypes())
                    .map(parameterType -> contextConfig.getContext().get(parameterType).orElseThrow(() -> new DependencyNotFoundException(parameterType, Collections.emptyList())))
                    .toArray();
            return constructor.newInstance(constructorParameters);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return Arrays.asList(constructor.getParameterTypes());
    }
}
