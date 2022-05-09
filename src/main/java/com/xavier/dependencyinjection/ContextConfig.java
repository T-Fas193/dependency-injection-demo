package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {

    interface Context {

        <T> Optional<T> get(Class<T> componentClass);

    }

    interface ComponentProvider<T> {

        T get();

        List<Class<?>> getDependencies();

    }

    private final Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public Context getContext() {
        providers.forEach((key, value) -> value.getDependencies().forEach(dependency -> {
            if (!providers.containsKey(dependency))
                throw new DependencyNotFoundException(key, Collections.singletonList(dependency));
        }));
        providers.keySet().forEach(key -> checkCyclicDependency(key, new ArrayDeque<>()));

        return new Context() {
            @Override
            public <T> Optional<T> get(Class<T> componentClass) {
                return Optional.ofNullable(providers.get(componentClass)).map(provider -> (T) provider.get());
            }
        };
    }

    private void checkCyclicDependency(Class<?> component, Deque<Class<?>> deque) {
        List<Class<?>> dependencies = providers.get(component).getDependencies();
        dependencies.forEach(dependency -> {
            if (deque.contains(dependency))
                throw new CyclicDependencyFoundException(dependency, new ArrayList<>(deque));
            deque.push(dependency);

            checkCyclicDependency(dependency, deque);

            deque.pop();
        });
    }


    public <T, I extends T> void bind(Class<T> typeClass, I implementationInstance) {
        providers.put(typeClass, new ComponentProvider<>() {
            @Override
            public Object get() {
                return implementationInstance;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return Collections.emptyList();
            }
        });
    }

    public <T, I extends T> void bind(Class<T> typeClass, Class<I> implementationClass) {
        providers.put(typeClass, new DefaultComponentProvider<>(implementationClass));
    }

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
        public T get() {
            try {
                Object[] constructorParameters = stream(constructor.getParameterTypes())
                        .map(parameterType -> getContext().get(parameterType).orElseThrow(() -> new DependencyNotFoundException(parameterType, Collections.emptyList())))
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

}
