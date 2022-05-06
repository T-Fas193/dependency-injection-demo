package com.xavier.dependencyinjection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CyclicDependencyFoundException extends RuntimeException {

    private final List<Class<?>> dependencies = new ArrayList<>();

    public CyclicDependencyFoundException(Class<?> dependency) {
        dependencies.add(dependency);
    }

    public CyclicDependencyFoundException(Class<?> dependency, List<Class<?>> existDependencies) {
        dependencies.add(dependency);
        dependencies.addAll(existDependencies);
    }

    public List<Class<?>> getDependencies() {
        return dependencies;
    }

    @Override
    public String getMessage() {
        String message = Optional.ofNullable(super.getMessage()).orElse("");
        String cyclicMessage = dependencies.stream().map(Class::getSimpleName).collect(Collectors.joining(" -> "));
        return message + cyclicMessage;
    }
}
