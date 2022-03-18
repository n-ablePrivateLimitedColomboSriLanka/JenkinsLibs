def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent { label 'ACE12' }
        options {
            skipDefaultCheckout(true)
        }
        stages {
            stage('Build') {
                steps {
                    cleanWs()
                    checkout scm
                    sh '/pipelinescript/build.sh ' + pipelineParams.sharedLib
                }
            }

            stage('Deploy') {
                steps {
                    sh '/pipelinescript/deploy.sh ' + "${pipelineParams.nodeSpec} ${pipelineParams.server}"
                }
            }
        }
    }
}