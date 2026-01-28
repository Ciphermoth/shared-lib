def call(Map config = [:]) {

    // Load helper class from src/release/ReleaseHelpers.groovy
    def helpers = new release.ReleaseHelpers(this)

    pipeline {
        agent any

        parameters {
            booleanParam(
                name: 'IS_CANDIDATE',
                defaultValue: true,
                description: 'Check to create a release-candidate. Uncheck to create an official release.'
            )

            booleanParam(
                name: 'DEPLOY_TO_ENV',
                defaultValue: false,
                description: 'Optional: Deploy nt## to ### Dev environment after release creation.'
            )
        }

        stages {

            stage('Checkout All Repositories') {
                steps {
                    script {
                        echo "=== CHECKOUT STAGE ==="
                        helpers.checkoutAllComponents()
                    }
                }
            }

            stage('Load release-version.json') {
                steps {
                    script {
                        echo "=== RELEASE CREATION STAGE ==="
                        helpers.loadReleaseVersion()
                        helpers.determineVersionChanges()
                    }
                }
            }

            stage('Create Release Candidate Branch') {
                when { expression { params.IS_CANDIDATE } }
                steps {
                    script {
                        echo "=== CREATING RELEASE CANDIDATE ==="
                        helpers.createReleaseCandidateBranch()
                    }
                }
            }

            stage('Prepare Official Release Branch') {
                when { expression { !params.IS_CANDIDATE } }
                steps {
                    script {
                        echo "=== CREATING OFFICIAL RELEASE ==="
                        helpers.createOfficialReleaseBranch()
                    }
                }
            }

            stage('Generate Release Metadata Files') {
                when { expression { !params.IS_CANDIDATE } }
                steps {
                    script {
                        echo "=== GENERATING METADATA FILES ==="
                        helpers.generateMetadataFiles()
                    }
                }
            }

            stage('Pull #### Release Branch') {
                steps {
                    script {
                        echo "=== PULLING #### SUBMODULES ==="
                        helpers.pullDeploySubmodules(params.IS_CANDIDATE)
                    }
                }
            }

            stage('Generate Release Notes') {
                steps {
                    script {
                        echo "=== GENERATING RELEASE NOTES ==="
                        helpers.generateReleaseNotes()
                    }
                }
            }

            stage('Nexus Artifact Publishing') {
                steps {
                    script {
                        echo "=== NEXUS STAGE ==="
                        helpers.generateArtifacts()
                        helpers.uploadArtifactsToNexus()
                    }
                }
            }

            stage('Deploy to ### Dev Environment') {
                when { expression { params.DEPLOY_TO_ENV } }
                steps {
                    script {
                        echo "=== DEPLOY STAGE ==="
                        helpers.runDeployment()
                    }
                }
            }

            stage('Cleanup') {
                steps {
                    script {
                        echo "=== CLEANUP STAGE ==="
                        helpers.cleanTheWS()
                    }
                }
            }
        }

        post {
            success { echo "Create Release Pipeline completed successfully" }
            failure { echo "Create Release Pipeline failed" }
        }
    }
}
