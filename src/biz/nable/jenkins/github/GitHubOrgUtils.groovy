package biz.nable.jenkins.github

import org.kohsuke.github.GitHub 
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHRepository
import org.kohsuke.github.PagedIterable
import org.jenkinsci.plugins.github_branch_source.Connector
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.groovy.cps.NonCPS

class GitHubOrgUtils implements java.io.Serializable {
    private String orgName;
    private String apiUri;
    private String credentialsId;
    private transient GitHub github;
    private transient GHOrganization org;
 
    GitHubOrgUtils(String apiUri, String credentialsId, String orgName) {
        this.orgName = orgName;
        this.apiUri = apiUri;
        this.credentialsId = credentialsId;
        initializeObjects();
    }

    public List<String> getAllRepositories() {
        PagedIterable<GHRepository> repos = this.org.listRepositories(30);
        List<String> repositories = new ArrayList<String>();
        for (GHRepository ghRepo: repos) {
            repositories.add([
                repository_name: ghRepo.getName(),
                repository_full_name: ghRepo.getFullName(),
                repository_clone_url: ghRepo.getHttpTransportUrl()
            ]);
        }
        return repositories;
    }

    private void readObject(java.io.ObjectInputStream inputStream)
        throws IOException, ClassNotFoundException {
        inputStream.defaultReadObject();
        initializeObjects();
    }

    @NonCPS
    private void initializeObjects() {
        this.github = this.getGitHub(this.apiUri, this.credentialsId);
        this.org = this.github.getOrganization(this.orgName);
    }

    @NonCPS
    private GitHub getGitHub(String apiUri, String credentialsId) {
        StandardCredentials credentials = Connector.lookupScanCredentials(null, apiUri, 'jenkins_github_app', null);
        return Connector.connect(apiUri, credentials);
    }
}
