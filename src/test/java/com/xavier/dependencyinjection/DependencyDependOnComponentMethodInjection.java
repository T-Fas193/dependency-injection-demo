package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public class DependencyDependOnComponentMethodInjection implements Dependency {
    private Component component;

    public Component getComponent() {
        return component;
    }

    @Inject
    public void setComponent(Component component) {
        this.component = component;
    }
}
