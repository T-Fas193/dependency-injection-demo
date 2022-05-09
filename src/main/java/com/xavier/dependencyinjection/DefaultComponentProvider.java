package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static com.xavier.dependencyinjection.ContextConfig.ComponentProvider;
import static com.xavier.dependencyinjection.ContextConfig.Context;
import static java.util.Arrays.stream;

class DefaultComponentProvider<T> implements ComponentProvider<T> {

    private final Constructor<T> constructor;

    DefaultComponentProvider(Class<T> implementationClass) {
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
    public T get(Context context) {
        try {
            Object[] constructorParameters = stream(constructor.getParameterTypes())
                    .map(parameterType -> context.get(parameterType).orElse(null))
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
