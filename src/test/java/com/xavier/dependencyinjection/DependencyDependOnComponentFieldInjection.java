package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public class DependencyDependOnComponentFieldInjection implements Dependency {

    @Inject
    private Component component;

}
