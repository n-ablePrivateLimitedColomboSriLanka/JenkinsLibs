package biz.nable.jenkins.github

import org.kohsuke.github.GitHub 
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHRepository
import org.kohsuke.github.PagedIterable

class GitHubOrgUtils {
    private GitHub github;
    private String orgName;
    private GHOrganization org;
 
    GitHubOrgUtils(GitHub github, String orgName) {
        this.github = github;
        this.orgName = orgName;
        this.org = this.github.getOrganization(orgName);
    }

    List<String> getAllRepositories() {
        PagedIterable<GHRepository> repos = this.org.listRepositories(30)
        List<String> repositories = new ArrayList<String>()
        for (GHRepository ghRepo: repos) {
            repositories.add([
                repository_name: ghRepo.getName(),
                repository_full_name: ghRepo.getFullName(),
                repository_clone_url: ghRepo.getHttpTransportUrl()
            ])
        }
        return repositories
    }

    String sayHello() {
        println "Hello";
    }
}
