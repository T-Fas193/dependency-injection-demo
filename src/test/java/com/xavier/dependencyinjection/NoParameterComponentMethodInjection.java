package com.xavier.dependencyinjection;

import jakarta.inject.Inject;

public class NoParameterComponentMethodInjection implements Component {

    private String testString;

    public String getTestString() {
        return testString;
    }

    @Inject
    public void setTestString() {
        this.testString = "this is a test string";
    }
}
