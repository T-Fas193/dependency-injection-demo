package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public record StringConstructorDependency(String stringComponent) implements Dependency {

    @Inject
    public StringConstructorDependency {
    }
}
