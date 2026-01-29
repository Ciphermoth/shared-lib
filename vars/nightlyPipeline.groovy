
@Library('shared-lib') _

import release.NightlyHelpers
/**
* Nightly Build Pipeline - orchestrates
* checkout, build, deploy, test, scan, merge, and cleanup
**/
nightlyPipeline()


def call(Map config = [:]) {
// set configuration defaults here
    def repositories = config.repositories ?: []
    def dockerRegistry = config.dockerRegistry ?: 'localhost:5000'
    def bitbucketProject = config.bitbucketProject ?: 'MYPROJECT'
    def bitbucketUrl = config.bitbucketUrl ?: ''
    def nexusUrl = config.nexusUrl ?: ''
    def devBranch = config.devBranch ?: 'dev'
    def masterBranch = config.masterBranch ?: 'master'
    // minimal deploy single server agent
    def serverAgent = config.serverAgent ?: 'server-agent'  // minimal deploy single server agent
    def gitCredentialsId = config.gitCredentialsId ?: 'bitbucket-ssh-key'
    def sonarqubeServer = config.sonarqubeServer ?: 'SonarQubeServer'
    def composeFile = config.composeFile ?: 'docker-compose-nightly.yml'
    
    // runtime tracking vars
    // def builtImages = []
    def deploymentId = "nightly-${env.BUILD_NUMBER}"
    def mergeSuccessful = false
    def nightlyHelpers = null

    pipeline {
        agent none // specifify per stage ?

        triggers {
            cron('0 2 * * *')
        }

        // we can change this config this is just an example
        options {
            buildDiscarder(logRotator(numToKeepStr: '30', daysToKeepStr: '30'))
            timeout(time: 4, unit: 'HOURS')
            timestamps()
            disableConcurrentBuilds()
            skipDefaultCheckout(true)
        }

        environment {
            DOCKER_REGISTRY = "${dockerRegistry}"
            BITBUCKET_PROJECT = "${bitbucketProject}"
            DEV_TAG = 'dev'
            MASTER_TAG = 'master'
        }

        stages {
            // checkout
            stage('Checkout') {
                agent { label 'docker' }
                steps {
                    script {
                        echo "|| STAGE 1: Checkout - Pulling dev branches ||"
                        cleanWs()

                        nightlyHelpers = new NightlyHelpers(this)

                        nightlyHelpers.checkoutAllRepositories(
                            repositories,
                            devBranch,
                            bitbucketUrl, bitbucketProject,
                            gitCredentialsId
                        )

                        nightlyHelpers.updateAllBitbucketStatuses(
                            'INPROGRESS',
                            'NIGHTLY',
                            'Nightly Pipeline',
                            'Nightly build in progress'
                        )
                    }
                }
            }
            // docker image build
            stage('Docker Image') {
                agent { label 'docker' }
                steps {
                    script {
                        echo "|| STAGE 2: Docker Image Build - Building all docker images ||"
                        if (!nightlyHelpers) {
                            nightlyHelpers = new NightlyHelpers(this)
                        }


                        nightlyHelpers.buildAllDockerImages(
                            repositories,
                            env.DEV_TAG,
                            env.DOCKER_REGISTRY,
                            env.BUILD_NUMBER
                        )
                        nightlyHelpers.pushAllDockerImages()
                    }
                }
            }
            // minimal deploy
            stage('Minimal Deploy') {
                // dedicated server agent
                agent { label serverAgent }
                steps {
                    script {
                        echo "|| STAGE 3: Minimal Deploy - Deploying minimal stack ||"
                        if (!nightlyHelpers) {
                            nightlyHelpers = new NightlyHelpers(this)
                        }
                        // reconstruct built images list if on different agent
                        repositories.each { repo ->
                            nightlyHelpers.setBuiltImage( repo, "${env.DOCKER_REGISTRY}/${repo}:${env.DEV_TAG}")
                        }
                        nightlyHelpers.deployMinimalStack(deploymentId, composeFile)
                        nightlyHelpers.verifyDeploymentHealth(deploymentId)
                    }
                }
            }
            // test
            stage('Test') {
                agent {label serverAgent }
                steps {
                    script {
                        echo "|| STAGE 4: Test ||"
                        if (!nightlyHelpers) {
                            nightlyHelpers = new NightlyHelpers(this)
                        }
                        try {
                            nightlyHelpers.runTests(deploymentId, 'regression')
                            nightlyHelpers.runTests(deploymentId, 'integration')

                            runSmokeTests(deploymentId: deploymentId, testSuite: 'nightly')
                        } finally {
                            nightlyHelpers.publishTestResults()
                        }
                    }
                }
            }
            // scan
            stage('Scan') {
                agent { label 'docker' }
                steps {
                    script {
                        echo "|| STAGE 5: Scan - Running security scans ||"
                        if (!nightlyHelpers) {
                            nightlyHelpers = new NightlyHelpers(this)
                        }
                        repositories.each { repo ->
                            nightlyHelpers.setBuiltImage(repo, "${env.DOCKER_REGISTRY}/${repo}:${env.DEV_TAG}")
                        }

                        nightlyHelpers.runAllSecurityScans(repositories, devBranch, sonarqubeServer)
                    }
                }
            }
            // merge
            stage('Merge') {
                agent { label 'docker' }
                steps {
                    script {
                        echo "|| STAGE 6: Merge ||"
                        if (!nightlyHelpers) {
                            nightlyHelpers = new NightlyHelpers(this)
                        }

                        repositories.each { repo -> 
                            dir(repo) {
                                checkout({
                                    $class: 'GitSCM',
                                    branches: [[name: "*/${devBranch}"]],
                                    extensions: [[$class: 'CleanBeforeCheckout']],
                                    userRemoteConfigs: [[
                                        url: "${bitbucketUrl}/scm/${bitbucketProject}/${repo}.git",
                                        credentialsId: gitCredentialsId
                                    ]]
                                })
                            }
                        }

                        nightlyHelpers.mergeAllBranches(
                            repositories,
                            devBranch,
                            masterBranch,
                            gitCredentialsId,
                            env.BUILD_NUMBER
                        )

                        mergeSuccessful = true
                    }
                }
            }
            // nexus
            stage('Nexus') {
                agent { label 'docker' }
                steps {
                    script {
                        echo "|| STAGE 7: Nexus - Tagging and Pushing images ||"
                        if (!nightlyHelpers) {
                            nightlyHelpers = new NightlyHelpers(this)
                        }
                        repositories.each { repo -> 
                            nightlyHelpers.setBuiltImage( repo, "${env.DOCKER_REGISTRY}/${repo}:${env.DEV_TAG}")
                        }
                        nightlyHelpers.retagAndPushImages(
                            env.DEV_TAG,
                            env.MASTER_TAG,
                            env.DOCKER_REGISTRY
                        )
                    }
                }
            }
        }

        post {
            always {
                // cleanup
                node(serverAgent) {
                    script {
                        echo "|| STAGE 8: Cleanup - Cleaning up deployment ||"
                        def cleanupHelper = new NightlyHelpers(this)

                        repositories.each { repo -> 
                            cleanupHelper.setBuiltImage( repo, "${env.DOCKER_REGISTRY}/${repo}:${env.DEV_TAG}")
                        }
                        cleanupHelper.fullCleanup(deploymentId, composeFile)
                    }
                }
            }
            success {
                script {
                    echo "|| NIGHTLY PIPELINE COMPLETED SUCCESSFULLY ||"
                    if (nightlyHelpers) {
                        nightlyHelpers.updateAllBitbucketStatuses(
                            'SUCCESSFUL',
                            'NIGHTLY',
                            'Nightly Pipeline',
                            'Nightly build completed successfully - merged to master'
                        )
                    }
                }
            }
            failure {
                script {
                    echo "|| NIGHTLY PIPELINE FAILED ||"
                    if (nightlyHelpers) {
                        nightlyHelpers.updateAllBitbucketStatuses(
                            'FAILED',
                            'NIGHTLY',
                            'Nightly Pipeline',
                            'Nightly build failed - check Jenkins for details'
                        )
                    }
                }
            }
        }
    }
}
    