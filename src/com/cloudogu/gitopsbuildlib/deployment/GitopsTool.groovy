package com.cloudogu.gitopsbuildlib.deployment

enum GitopsTool {
    FLUX, ARGO

    // Creating enums without constructor results in Exception on Jenkins:
    // "RejectedAccessException: Scripts not permitted to use new java.util.LinkedHashMap" 🙄
    GitopsTool() {}

    // valueOf() does not work on Jenkins, so create our own
    static GitopsTool get(String potentialTool) {
        return values().find { it.name() == potentialTool }
    }
}
