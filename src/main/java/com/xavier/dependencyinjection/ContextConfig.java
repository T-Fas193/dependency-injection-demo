package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.stream;

public class ContextConfig {

    interface Context {

        <T> Optional<T> get(Class<T> componentClass);

    }

    interface ComponentProvider<T> {

        T get();

        void check();

    }

    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public Context getContext() {
        providers.forEach((key, value) -> value.check());

        return new Context() {
            @Override
            public <T> Optional<T> get(Class<T> componentClass) {
                return Optional.ofNullable(providers.get(componentClass)).map(provider -> (T) provider.get());
            }
        };
    }


    public <T, I extends T> void bind(Class<T> typeClass, I implementationInstance) {
        providers.put(typeClass, new ComponentProvider<>() {
            @Override
            public Object get() {
                return implementationInstance;
            }

            @Override
            public void check() {
                // no implementation needs here
            }
        });
    }

    public <T, I extends T> void bind(Class<T> typeClass, Class<I> implementationClass) {
        Constructor<?> constructor = getInjectionConstructor(implementationClass);
        providers.put(typeClass, new DefaultComponentProvider<>(constructor, typeClass));
    }

    class DefaultComponentProvider<T> implements ComponentProvider<T> {

        private final Constructor<T> constructor;
        private final Class<?> typeClass;

        private boolean constructing = false;

        private boolean checking = false;

        DefaultComponentProvider(Constructor<T> constructor, Class<?> typeClass) {
            this.constructor = constructor;
            this.typeClass = typeClass;
        }

        @Override
        public T get() {
            if (constructing) throw new CyclicDependencyFoundException(typeClass);
            try {
                constructing = true;
                Object[] constructorParameters = stream(constructor.getParameterTypes())
                        .map(parameterType -> getContext().get(parameterType).orElseThrow(() -> new DependencyNotFoundException(parameterType, Collections.emptyList())))
                        .toArray();
                return constructor.newInstance(constructorParameters);
            } catch (CyclicDependencyFoundException e) {
                throw new CyclicDependencyFoundException(typeClass, e.getDependencies());
            } catch (DependencyNotFoundException e) {
                throw new DependencyNotFoundException(typeClass, e.getDependencies());
            } catch (ReflectiveOperationException e) {
                throw new UnsupportedOperationException(e);
            } finally {
                constructing = false;
            }
        }

        @Override
        public void check() {
            if (checking) throw new CyclicDependencyFoundException(typeClass);
            try {
                checking = true;
                stream(constructor.getParameterTypes()).forEach(parameterType -> Optional.ofNullable(providers.get(parameterType))
                        .orElseThrow(() -> new DependencyNotFoundException(parameterType, Collections.emptyList()))
                        .check());
            } catch (CyclicDependencyFoundException e) {
                throw new CyclicDependencyFoundException(typeClass, e.getDependencies());
            } catch (DependencyNotFoundException e) {
                throw new DependencyNotFoundException(typeClass, e.getDependencies());
            } finally {
                checking = false;
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
