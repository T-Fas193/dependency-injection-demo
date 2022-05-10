package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public class ComponentDependOnDependencyMethodInjection implements Component {

    private Dependency dependency;


    public Dependency getDependency() {
        return dependency;
    }

    @Inject
    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
    }

}
