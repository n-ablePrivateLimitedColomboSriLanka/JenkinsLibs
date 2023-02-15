def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent any
        environment {
           APPLICATION_DEFAULT_DIR='APPLICATION_5JITHW8YDN7KSW'
        }
        options {
            skipDefaultCheckout(true)
            buildDiscarder(logRotator(numToKeepStr: '-1', artifactNumToKeepStr: '2'))
        }
        parameters {
            string(name: 'releaseBranch', defaultValue: pipelineParams.releaseBranchDefaultValue)
            text(name: 'artifactRepositoriesJSON', defaultValue: pipelineParams.artifactRepositoriesJSONDefaultValue)
            string(name: 'gitRepositoryUrl', defaultValue: pipelineParams.gitRepositoryUrlDefaultValue)
            string(name: 'sharedLibIndexUrl', defaultValue: pipelineParams.sharedLibIndexUrlDefaultValue)
            string(name: 'githubCredentialsId', defaultValue: pipelineParams.githubCredentialsIdDefaultValue)
            string(name: 'aceBuildScriptsRepository', defaultValue: pipelineParams.aceBuildScriptsRepositoryDefaultValue)
        }
        stages {
            stage('Checkout SCM') {
                steps {
                    cleanWs()
                    script {
                        scmvars = checkout scmGit(
                                        branches: [[name: "*/${params.releaseBranch}"]],
                                        extensions: [
                                            cloneOption(depth: 1, noTags: true, reference: '', shallow: true),
                                            [$class: 'IgnoreNotifyCommit'],
                                            [$class: 'RelativeTargetDirectory', relativeTargetDir: env.APPLICATION_DEFAULT_DIR]
                                        ],
                                        userRemoteConfigs: [
                                            [credentialsId: params.githubCredentialsId, url: params.gitRepositoryUrl]
                                        ]
                                 )
                    }
                }
            }

            stage('Checkout Build Scripts') {
                steps {
                    checkout scmGit(
                                    branches: [[name: "*/master"]],
                                    extensions: [
                                        cloneOption(depth: 1, noTags: true, reference: '', shallow: true),
                                        [$class: 'IgnoreNotifyCommit'],
                                        [$class: 'RelativeTargetDirectory', relativeTargetDir: "buildscripts"]
                                    ],
                                    userRemoteConfigs: [
                                        [credentialsId: params.githubCredentialsId, url: params.aceBuildScriptsRepository]
                                    ]
                             )

                }
            }

            stage('Build') {
                agent {
                    docker {
                        image 'ireshmm/ace-builder:12.0.2'
                        reuseNode true
                    }
				}
                steps {
                    withCredentials([gitUsernamePassword(credentialsId: params.githubCredentialsId, gitToolName: 'Default')]) {
                        sh 'buildscripts/ace/build.sh ' + params.sharedLibIndexUrl + ' ' + params.releaseBranch
                    }
                }
            }
            
            stage('Deploy Artifact') {
                steps {
                    script {
                        artifactRepos = readJSON text: params.artifactRepositoriesJSON
                        artifactRepos.repositories.each { repository -> 
                            withMaven(maven: 'maven3', mavenSettingsConfig: 'maven3-settings', publisherStrategy: 'EXPLICIT', options: [artifactsPublisher(disabled: false)]) {
                                sh "mvn deploy:deploy-file -DrepositoryId=${repository.id} -Durl=${repository.url} -Dfile=`echo *.bar` -DpomFile=${env.APPLICATION_DEFAULT_DIR}/pom.xml -Dpackaging=bar"
                            }
                        }
                    }
                }
            }
            
            stage('Archive artifact') {
                steps {
                    script {
                        appName = readFile('appname').trim()
                        commit = scmvars.GIT_COMMIT.substring(0,7)
                        now = new Date()
                        timestamp = now.format("yyyy-MM-dd-HH-mm-ss", TimeZone.getTimeZone("GMT+5:30"))
                        timestampedFileName = "${appName}_${commit}_${timestamp}.bar"
                    }

                    sh "cp ${appName}.bar ${timestampedFileName}"
                    archiveArtifacts artifacts:timestampedFileName, fingerprint: true
                }
            }
        }
    }
}
