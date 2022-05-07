package com.xavier.dependencyinjection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DependencyNotFoundException extends RuntimeException {

    private final List<Class<?>> dependencies = new ArrayList<>();

    public DependencyNotFoundException(Class<?> componentType, Class<?> implementationType) {
        dependencies.add(componentType);
        dependencies.add(implementationType);
    }

    public List<Class<?>> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    @Override
    public String getMessage() {
        String message = Optional.ofNullable(super.getMessage()).orElse("");
        String dependencyFlow = dependencies.stream().map(Class::getSimpleName).collect(Collectors.joining(" -> "));
        return message.concat(dependencyFlow).concat(" not found");
    }
}
