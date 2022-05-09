package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public class ComponentDependOnDependencyFieldInjection implements Component {

    @Inject
    private Dependency dependency;

    public Dependency getDependency() {
        return dependency;
    }

}
