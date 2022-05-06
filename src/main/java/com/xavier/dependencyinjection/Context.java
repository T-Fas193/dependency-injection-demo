package com.xavier.dependencyinjection;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.stream;

public class Context {

    private final Map<Class<?>, Provider<?>> providers = new HashMap<>();
    private final Map<Class<?>, Constructor<?>> constructors = new HashMap<>();

    public <T> Optional<T> get(Class<T> componentClass) {
        return Optional.ofNullable(providers.get(componentClass)).map(provider -> (T) provider.get());
    }


    public <T, I extends T> void bind(Class<T> typeClass, I implementationInstance) {
        providers.put(typeClass, () -> implementationInstance);
    }

    public <T, I extends T> void bind(Class<T> typeClass, Class<I> implementationClass) {
        Constructor<?> constructor = getInjectionConstructor(implementationClass);
        constructors.put(typeClass, constructor);
        providers.put(typeClass, () -> {
            try {
                Object[] constructorParameters = stream(constructor.getParameterTypes())
                        .map(parameterType -> checkCyclicDependency(typeClass, parameterType))
                        .map(parameterType -> get(parameterType).orElseThrow(DependencyNotExists::new))
                        .toArray();
                return constructor.newInstance(constructorParameters);
            } catch (ReflectiveOperationException e) {
                throw new UnsupportedOperationException(e);
            }
        });
    }

    private <T> Class<?> checkCyclicDependency(Class<T> typeClass, Class<?> parameterType) {
        if (!constructors.containsKey(parameterType)) return parameterType;

        Constructor<?> parameterConstructor = constructors.get(parameterType);
        stream(parameterConstructor.getParameterTypes())
                .filter(parameterConstructorInjection -> parameterConstructorInjection.equals(typeClass))
                .findAny().ifPresent(cyclicClass -> {
                    throw new CyclicDependencyFoundException();
                });
        return parameterType;
    }

    private Constructor<?> getInjectionConstructor(Class<?> implementationClass) {
        if (classCannotInstance(implementationClass)) throw new UnsupportedOperationException();

        Constructor<?>[] injectionConstructors = stream(implementationClass.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
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

    private boolean classCannotInstance(Class<?> implementationClass) {
        int modifiers = implementationClass.getModifiers();
        return Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers);
    }

}
