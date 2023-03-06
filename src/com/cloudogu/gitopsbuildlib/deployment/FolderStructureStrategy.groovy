package com.cloudogu.gitopsbuildlib.deployment

/**
 * Determines which folder structure strategy shall be used.
 * Read more about this topic here: https://github.com/cloudogu/gitops-patterns#release-promotion
 */
enum FolderStructureStrategy {
    /**
     * Uses subfolders for each stage at the root path, with all application-folders located in one of these.
     * <br>
     * <br>
     * Example:
     * <ul>
     *   <li>$ROOTPATH/staging/myapp/</li>
     *   <li>$ROOTPATH/production/myapp/</li>
     * </ul>
     */
    GLOBAL_ENV('globalEnv'),

    /**
     * Uses subfolders for each application at the root path, with all subfolders per stage in each of them.
     * <br>
     * <br>
     * Example:
     * <ul>
     *   <li>$ROOTPATH/myapp/staging/</li>
     *   <li>$ROOTPATH/myapp/production/</li>
     * </ul>
     */
    ENV_PER_APP('envPerApp')

    private final String name

    FolderStructureStrategy(String name) {
        this.name = name
    }

    String getNameValue() {
        return name
    }


    String toString() {
        return name() + " = " + getNameValue()
    }
}
