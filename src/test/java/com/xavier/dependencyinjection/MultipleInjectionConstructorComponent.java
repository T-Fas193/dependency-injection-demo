package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public class MultipleInjectionConstructorComponent implements Component {

    @Inject
    @SuppressWarnings("unused")
    public MultipleInjectionConstructorComponent(String firstInjection) {
    }

    @Inject
    @SuppressWarnings("unused")
    public MultipleInjectionConstructorComponent(String firstInjection, String secondInjection) {
    }

}
