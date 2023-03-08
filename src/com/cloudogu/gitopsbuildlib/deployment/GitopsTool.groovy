package com.cloudogu.gitopsbuildlib.deployment

enum GitopsTool {
    FLUX, ARGO

    static boolean isValid(String potentialTool) {
        return values().any { it.toString().equals(potentialTool) }
    }
}
