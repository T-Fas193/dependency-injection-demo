package com.xavier.dependencyinjection;

import java.util.*;

public class ContextConfig {

    interface Context {

        <T> Optional<T> get(Class<T> componentClass);

    }

    interface ComponentProvider<T> {

        T get(Context context);

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
                return Optional.ofNullable(providers.get(componentClass)).map(provider -> (T) provider.get(this));
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
            public Object get(Context context) {
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

}
