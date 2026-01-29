
@Library('shared-lib') _

/**
* Nightly Build Pipeline - orchestrates
* checkout, build, deploy, test, scan, merge, and cleanup
**/
nightlyPipeline()


def call(Map config = [:]) {
/** set configuration defaults here
*   def repositories = config.repositories ?: []
*   def dockerRegistry = config.dockerRegistry ?: 'localhost:5000'
*   def nexusUrl = config.nexusUrl ?: ''
*   def devBranch = config.devBranch ?: 'dev'
*   def masterBranch = config.masterBranch ?: 'master'
*   def serverAgent = config.serverAgent ?: 'server-agent'  // minimal deploy single server agent
**/

    def builtImages = []
    def deploymentId = "nightly-${env.BUILD_NUMBER}"

    pipeline {
        agent none // specifify per stage ?

        triggers {
            cron('0 2 * * *')
        }

        // we can change this config this is just an example
        options {
            buildDiscarder(logRotator(numToKeepStr: '30'))
            timeout(time: 4, unit: 'HOURS')
            timestamps()
            disableConcurrentBuilds()
        }

        environment {
            DOCKER_REGISTRY = "${dockerRegistry}"
            DEV_TAG = 'dev'
            MASTER_TAG = 'master'
        }

        stages {
            // checkout
            stage('Checkout') {
                agent { label 'docker' }
                steps {
                    script {
                        checkoutAllRepositories(repositories, devBranch)
                    }
                }
            }
            // docker image build
            stage('Docker Image') {
                agent { label 'docker' }
                steps {
                    script {
                        builtImages = buildAllDockerImages(repositories, 'dev')
                    }
                }
            }
            // minimal deploy
            stage('Minimal Deploy') {
                // dedicated server agent
                agent { label serverAgent }
                steps {
                    script {
                        deployMinimalStack(deploymentId, builtImages)
                    }
                }
            }
            // test
            stage('Test') {
                agent {label serverAgent }
                steps {
                    script {
                        runTestSuite(deploymentId)
                    }
                }
            }
            // scan
            stage('Scan') {
                agent { label 'docker' }
                steps {
                    script {
                        runSecurityScans(repositories, builtImages)
                    }
                }
            }
            // merge
            stage('Merge') {
                agent { label 'docker' }
                steps {
                    script {
                        mergeDevToMaster(repositories, devBranch, masterBranch)
                    }
                }
            }
            // nexus
            stage('Nexus') {
                agent { label 'docker' }
                steps {
                    script {
                        tagAndPushMasterImages(builtImages, nexusUrl)
                    }
                }
            }
        }

        post {
            always {
                // cleanup
                node(serverAgent) {
                    script {
                        cleanupDeployment(deploymentId, builtImages)
                    }
                }
            }
            success {
                script {
                    notifySuccess()
                }
            }
            failure {
                script {
                    notifyFailure()
                }
            }
        }
    }
}
    