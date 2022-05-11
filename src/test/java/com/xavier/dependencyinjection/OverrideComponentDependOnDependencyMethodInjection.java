package com.xavier.dependencyinjection;

public class OverrideComponentDependOnDependencyMethodInjection extends ComponentDependOnDependencyMethodInjection {

    private String testString;

    public String getTestString() {
        return testString;
    }

    @Override
    public void setDependency(Dependency dependency) {
        super.setDependency(dependency);
        testString = "this is a override test string";
    }
}
