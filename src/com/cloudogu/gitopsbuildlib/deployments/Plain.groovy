package com.cloudogu.gitopsbuildlib.deployments

class Plain implements Deployment{

    def config

    Plain(Map config) {
        this.config = config
    }

    @Override
    def getType() {
        'plain'
    }

    @Override
    def update() {
        echo 'hi i am plain'
    }

    @Override
    def config() {
        return config
    }
}
