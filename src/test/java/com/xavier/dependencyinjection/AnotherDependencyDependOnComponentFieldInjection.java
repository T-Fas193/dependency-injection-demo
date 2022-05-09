package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public class AnotherDependencyDependOnComponentFieldInjection implements AnotherDependency {

    @Inject
    private Component component;

}
