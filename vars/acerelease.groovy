def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent { label 'ACE12' }
        environment {
           APPLICATION_DEFAULT_DIR='APPLICATION_5JITHW8YDN7KSW'
        }
        options {
            skipDefaultCheckout(true)
            buildDiscarder(logRotator(numToKeepStr: '-1', artifactNumToKeepStr: '2'))
        }
        parameters {
            booleanParam(name: 'PRODUCTION_BUILD', defaultValue: false, description: '')
            string(name: 'PASSWORD', defaultValue: '', description: '')
            string(name: 'DEPLOY_INTERVAL', defaultValue: '10', description: 'Time between each deployment to multiple integration nodes')
        }
        stages {
            stage('Build') {
                steps {
                    cleanWs()
                    script {
                        scmvars = checkout scm: [$class: 'GitSCM', branches: scm.branches, extensions: scm.extensions + [[$class: 'RelativeTargetDirectory', relativeTargetDir: env.APPLICATION_DEFAULT_DIR]], userRemoteConfigs: scm.userRemoteConfigs]
                    }
                    sh '/pipelinescript/build.sh ' + pipelineParams.sharedLib + ' ' + env.BRANCH_NAME
                }
            }

            stage('Deploy') {
                when {
                    expression { (params.PRODUCTION_BUILD == true && params.PASSWORD == 's0a#123') || env.BRANCH_NAME != 'production' }
                }
                steps {
                    script {
                        if(pipelineParams.server?.trim()) {
                            serverList = [pipelineParams.server]
                        } else {
                            serverList = pipelineParams.serverList
                        }
                        environments = getIIBServer(env.BRANCH_NAME)
                        environments.eachWithIndex { environment, index ->
                            for (server in serverList) {
                                sh '/pipelinescript/deploy.sh ' + "'${environment}' ${server}"
                            }
                            if(index < environments.size() - 1) {
                                print "Waiting ${params.DEPLOY_INTERVAL} seconds until the deployment to next node"
                                sleep params.DEPLOY_INTERVAL as int
                            }
                        }
                    }
                }
            }

            stage('Archive artifact') {
                when {
                    expression { env.BRANCH_NAME == 'production' }
                }
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
