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
        }
        parameters {
            booleanParam(name: 'PRODUCTION_BUILD', defaultValue: false, description: '')
            string(name: 'PASSWORD', defaultValue: '', description: '')
        }
        stages {
            stage('Build') {
                steps {
                    print params.PRODUCTION_BUILD == true
                    print params.PASSWORD
                    print params.PASSWORD == 's0a#123'
                    cleanWs()
                    checkout scm: [$class: 'GitSCM', branches: scm.branches, extensions: scm.extensions + [[$class: 'RelativeTargetDirectory', relativeTargetDir: env.APPLICATION_DEFAULT_DIR]], userRemoteConfigs: scm.userRemoteConfigs]
                    sh '/pipelinescript/build.sh ' + pipelineParams.sharedLib + ' ' + env.BRANCH_NAME
                }
            }

            stage('Deploy') {
                when {
                    expression { (params.PRODUCTION_BUILD == true && params.PASSWORD == 's0a#123') || env.BRANCH_NAME != 'production' }
                }
                steps {
                   script {
                        def environments = getIIBServer(env.BRANCH_NAME)
                        for (environment in environments) {
                            for (server in pipelineParams.serverList) {
                                sh '/pipelinescript/deploy.sh ' + "'${environment}' ${server}"
                            }
                        }
                    }
                }
            }
        }
    }
}
