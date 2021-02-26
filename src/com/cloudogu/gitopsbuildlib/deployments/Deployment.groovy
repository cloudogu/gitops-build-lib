package com.cloudogu.gitopsbuildlib.deployments

interface Deployment {

    def getType()
    def update()
    def config()
}
