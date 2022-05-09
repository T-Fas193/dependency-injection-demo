package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public class DependencyDependOnAnotherDependencyFieldInjection implements Dependency {

    @Inject
    private AnotherDependency anotherDependency;

}
