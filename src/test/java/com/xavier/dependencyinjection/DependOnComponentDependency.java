package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public record DependOnComponentDependency(Component component) implements Dependency {
    @Inject
    public DependOnComponentDependency {
    }
}
