package com.cloudogu.gitopsbuildlib.deployment

enum SourceType {
    HELM, PLAIN

    // Creating enums without constructor results in Exception on Jenkins:
    // "RejectedAccessException: Scripts not permitted to use new java.util.LinkedHashMap" 🙄
    SourceType() {}
}
