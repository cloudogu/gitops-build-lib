package com.cloudogu.gitopsbuildlib.deployment

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
