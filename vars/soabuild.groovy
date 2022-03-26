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
        stages {
           stage('Build') {
               steps {
                   cleanWs()
                   checkout scm: [$class: 'GitSCM', branches: scm.branches, extensions: scm.extensions + [[$class: 'RelativeTargetDirectory', relativeTargetDir: env.APPLICATION_DEFAULT_DIR]], userRemoteConfigs: scm.userRemoteConfigs]
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
