package com.cloudogu.gitopsbuildlib.deployments

class Helm implements Deployment{

    static String getHelmImage() { 'ghcr.io/cloudogu/helm:3.4.1-1' }
    def config

    Helm(Map config) {
        this.config = config
    }

    @Override
    def getType() {
        'helm'
    }

    @Override
    def update() {
        echo 'hi i am helm'
    }

    @Override
    def config() {
        return config
    }
}
