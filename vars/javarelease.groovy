def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent any
        tools {
            jdk 'JDK8'
        }
        options {
            skipDefaultCheckout(true)
            buildDiscarder(logRotator(numToKeepStr: '-1', artifactNumToKeepStr: '2'))
        }
        parameters {
            string(name: 'releaseBranch', defaultValue: pipelineParams.releaseBranchDefaultValue)
            text(name: 'artifactRepositoriesJSON', defaultValue: pipelineParams.artifactRepositoriesJSONDefaultValue)
            string(name: 'gitRepositoryUrl', defaultValue: pipelineParams.gitRepositoryUrlDefaultValue)
            string(name: 'githubCredentialsId', defaultValue: pipelineParams.githubCredentialsIdDefaultValue)
        }
        stages {
            stage('Checkout SCM') {
                steps {
                    cleanWs()
                    sh 'java --version'
                    checkout scmGit(
                                 branches: [[name: "*/${params.releaseBranch}"]],
                                 extensions: [
                                     cloneOption(depth: 1, noTags: true, reference: '', shallow: true),
                                     [$class: 'IgnoreNotifyCommit']
                                 ],
                                 userRemoteConfigs: [
                                     [credentialsId: params.githubCredentialsId, url: params.gitRepositoryUrl]
                                 ]
                             )
                }
            }
            stage('Deploy Artifact') {
                steps {
                    withMaven(maven: 'maven3', mavenSettingsConfig: 'maven3-settings') {
                        sh "mvn clean package -Dmaven.test.skip=true"
                    }

                    script {
                        artifactRepos = readJSON text: params.artifactRepositoriesJSON
                        artifactRepos.repositories.each { repository -> 
                            withMaven(maven: 'maven3', mavenSettingsConfig: 'maven3-settings', publisherStrategy: 'EXPLICIT', options: [artifactsPublisher(disabled: false)]) {
                                sh "mvn deploy -DaltDeploymentRepository=${repository.id}::default::${repository.url} -Dmaven.test.skip=true"
                            }
                        }
                    }
                }
            }
        }
    }
}
