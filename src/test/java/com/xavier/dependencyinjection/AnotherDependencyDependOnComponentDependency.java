package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public record AnotherDependencyDependOnComponentDependency(Component component) implements AnotherDependency {

    @Inject
    public AnotherDependencyDependOnComponentDependency {
    }

}
