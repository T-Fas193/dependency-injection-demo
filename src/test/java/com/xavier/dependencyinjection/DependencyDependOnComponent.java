package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public record DependencyDependOnComponent(Component component) implements Dependency {
    @Inject
    public DependencyDependOnComponent {
    }
}
