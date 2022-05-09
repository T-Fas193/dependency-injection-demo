package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public class ComponentDependOnFinalDependencyFieldInjection implements Component {

    @Inject
    private final Dependency dependency = null;

}
