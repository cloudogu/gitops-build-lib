package com.cloudogu.gitopsbuildlib.validation

enum GitopsTool {
    FLUX('flux'), ARGO('argo')

    private final String name

    GitopsTool(String name) {
        this.name = name
    }

    String getNameValue() {
        return name
    }

    String toString() {
        return name() + " = " + getNameValue()
    }
}
