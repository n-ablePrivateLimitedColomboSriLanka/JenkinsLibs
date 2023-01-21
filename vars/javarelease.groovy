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
            string(name: 'releaseBranch', defaultValue: pipelineParams.releaseBranchDefaultValue)
            text(name: 'artifactRepositoriesJSON', defaultValue: pipelineParams.artifactRepositoriesJSONDefaultValue)
            string(name: 'gitRepositoryUrl', defaultValue: pipelineParams.gitRepositoryUrlDefaultValue)
            string(name: 'githubCredentialsId', defaultValue: pipelineParams.githubCredentialsIdDefaultValue)
        }
        stages {
            stage('Checkout SCM') {
                steps {
                    cleanWs()
                    git credentialsId: params.githubCredentialsId, url: params.gitRepositoryUrl, branch: params.releaseBranch
                }
            }
            stage('Deploy Artifact') {
                steps {
                    withMaven(maven: 'maven3', mavenSettingsConfig: 'maven3-settings') {
                        sh "mvn clean package"
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
