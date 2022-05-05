package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public record InjectionConstructorComponent(Dependency dependency) implements Component {

    @Inject
    public InjectionConstructorComponent {
    }
}
