package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public record DependencyDependOnAnotherDependency(AnotherDependency anotherDependency) implements Dependency {

    @Inject
    public DependencyDependOnAnotherDependency {
    }

}
