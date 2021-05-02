package com.cloudogu.gitopsbuildlib.validation

enum Deployment {
    HELM('helm'), PLAIN('plain')

    private final String name

    Deployment(String name) {
        this.name = name
    }

    String getNameValue() {
        return name
    }

    String toString() {
        return name() + " = " + getNameValue()
    }
}
