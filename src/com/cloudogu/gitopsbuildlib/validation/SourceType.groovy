package com.cloudogu.gitopsbuildlib.validation

enum SourceType {
    HELM('helm'), PLAIN('plain')

    private final String name

    SourceType(String name) {
        this.name = name
    }

    String getNameValue() {
        return name
    }

    String toString() {
        return name() + " = " + getNameValue()
    }
}
