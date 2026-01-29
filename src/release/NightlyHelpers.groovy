package release

class NightlyHelpers implements Serializable {

    def steps
    def builtImages = [:]
    def commitMap = [:]

    NightlyHelpers(steps) {
        this.steps = steps
    }

    // ---------------------------------------------------------
    // CHECKOUT LOGIC
    // ---------------------------------------------------------

    /**
        * Checkout all repos on their dev branches
        * @param repositories - list of repository names
        * @param branch - branch name to checkout (default should be dev)
        * @param bitbucketUrl - Bitbucket server URL
        * @param bitbucketProject - Bitbucket project key
        * @param credentialsId - Jenkins credentials ID for Bitbucket
        * @return commitMap - map of repository names to their checked out commit hashes
    */
    def checkoutAllDevBranches(List repositories, String branch, String bitbucketUrl, String bitbucketProject, String credentialsId) {
        steps.echo "Checking out ${branch} branch for ${repositories.size()} repos..."

        repositories.each { repo -> 
            steps.dir(repo) {
                steps.checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${branch}"]],
                    extensions: [
                        [$class: 'CleanBeforeCheckout'],
                        [$class: 'CloneOption', depth: 0, noTags: false, shallow: false ] // make sure this works
                    ]
                    userRemoteConfigs: [[
                        url: "${bitbucketUrl}/scm/${bitbucketProject}/${repo}.git",
                        credentialsId: credentialsId
                    ]]
                ])
                // commit hash
                def commitSha = steps.sh(
                    script: "git rev-parse HEAD",
                    returnStdout: true
                ).trim()

                commitMap[repo] = commitSha
                steps.echo "Checked out ${repo} at commit ${commitSha}"
            }
        }
        return commitMap
    }

