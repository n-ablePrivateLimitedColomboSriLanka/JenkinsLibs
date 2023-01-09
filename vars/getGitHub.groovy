import org.jenkinsci.plugins.github_branch_source.Connector

def call(def apiUri, credentialsId) {
    credentials = Connector.lookupScanCredentials(null, apiUri, 'jenkins_github_app', null)
    return Connector.connect(apiUri, credentials)
}
