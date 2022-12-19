def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent any
        options {
            skipDefaultCheckout(true)
            buildDiscarder(logRotator(numToKeepStr: '-1', artifactNumToKeepStr: '2'))
        }
        parameters {
            string(name: 'releaseBranch', defaultValue: 'master')
            string(name: 'artifactRepositoryUrl', defaultValue: pipelineParams.artifactRepositoryUrlDefaultValue)
            string(name: 'artifactRepositoryId', defaultValue: pipelineParams.artifactRepositoryIdDefaultValue)
            string(name: 'gitRepositoryUrl', defaultValue: pipelineParams.gitRepositoryUrlDefaultValue)
        }
        stages {
            stage('Checkout SCM') {
                steps {
                    cleanWs()
                    git credentialsId: 'jenkins_github_app', url: params.gitRepositoryUrl, branch: params.releaseBranch
                }
            }
            stage('Deploy Artifact') {
                steps {
                    withMaven(maven: 'maven3', mavenSettingsConfig: 'maven3-settings', publisherStrategy: 'EXPLICIT', options: [artifactsPublisher(disabled: false)]) {
                        sh "mvn clean package deploy -DaltDeploymentRepository=${params.artifactRepositoryId}::default::${params.artifactRepositoryUrl} -Dmaven.test.skip=true"
                    }
                }
            }
        }
    }
}
