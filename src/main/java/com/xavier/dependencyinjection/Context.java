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

    public <T> Optional<T> get(Class<T> componentClass) {
        return Optional.ofNullable(providers.get(componentClass)).map(provider -> (T) provider.get());
    }


    public <T, I extends T> void bind(Class<T> typeClass, I implementationInstance) {
        providers.put(typeClass, () -> implementationInstance);
    }

    public <T, I extends T> void bind(Class<T> typeClass, Class<I> implementationClass) {
        Constructor<?> constructor = getInjectionConstructor(implementationClass);
        providers.put(typeClass, new ComponentProvider<>(constructor, typeClass));
    }

    class ComponentProvider<T> implements Provider<T> {

        private final Constructor<T> constructor;
        private final Class<?> typeClass;

        private boolean constructing = false;

        ComponentProvider(Constructor<T> constructor, Class<?> typeClass) {
            this.constructor = constructor;
            this.typeClass = typeClass;
        }

        @Override
        public T get() {
            if (constructing) throw new CyclicDependencyFoundException(typeClass);
            try {
                constructing = true;
                Object[] constructorParameters = stream(constructor.getParameterTypes())
                        .map(parameterType -> Context.this.get(parameterType).orElseThrow(DependencyNotFoundException::new))
                        .toArray();
                return constructor.newInstance(constructorParameters);
            } catch (CyclicDependencyFoundException e) {
                throw new CyclicDependencyFoundException(typeClass, e.getDependencies());
            } catch (ReflectiveOperationException e) {
                throw new UnsupportedOperationException(e);
            } finally {
                constructing = false;
            }
        }
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
