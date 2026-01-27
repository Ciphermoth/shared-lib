
def call(Map config = [:]) {

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

            // ---------------------------------------------------------
            // CHECKOUT STAGE
            // ---------------------------------------------------------
            stage('Checkout All Repositories') {
                steps {
                    script {
                        echo "=== CHECKOUT STAGE ==="
                        checkoutAllComponents()
                    }
                }
            }

            // ---------------------------------------------------------
            // RELEASE CREATION STAGE
            // ---------------------------------------------------------
            stage('Load release-version.json') {
                steps {
                    script {
                        echo "=== RELEASE CREATION STAGE ==="
                        loadReleaseVersion()
                        determineVersionChanges()
                    }
                }
            }

            // ---------------------------------------------------------
            // RELEASE CANDIDATE LOGIC
            // ---------------------------------------------------------
            stage('Create Release Candidate Branch') {
                when { expression { params.IS_CANDIDATE } }
                steps {
                    script {
                        echo "=== CREATING RELEASE CANDIDATE ==="
                        createReleaseCandidateBranch()
                    }
                }
            }

            // ---------------------------------------------------------
            // OFFICIAL RELEASE LOGIC
            // ---------------------------------------------------------
            stage('Prepare Official Release Branch') {
                when { expression { !params.IS_CANDIDATE } }
                steps {
                    script {
                        echo "=== CREATING OFFICIAL RELEASE ==="
                        createOfficialReleaseBranch()
                    }
                }
            }

            stage('Generate Release Metadata Files') {
                when { expression { !params.IS_CANDIDATE } }
                steps {
                    script {
                        echo "=== GENERATING METADATA FILES ==="
                        generateMetadataFiles()
                    }
                }
            }

            // ---------------------------------------------------------
            // NTSPDDEPLOY SUBMODULE PULL
            // ---------------------------------------------------------
            stage('Pull #### Release Branch') {
                steps {
                    script {
                        echo "=== PULLING #### SUBMODULES ==="
                        pullDeploySubmodules(params.IS_CANDIDATE)
                    }
                }
            }

            // ---------------------------------------------------------
            // RELEASE NOTES
            // ---------------------------------------------------------
            stage('Generate Release Notes') {
                steps {
                    script {
                        echo "=== GENERATING RELEASE NOTES ==="
                        generateReleaseNotes()
                    }
                }
            }

            // ---------------------------------------------------------
            // NEXUS STAGE
            // ---------------------------------------------------------
            stage('Nexus Artifact Publishing') {
                steps {
                    script {
                        echo "=== NEXUS STAGE ==="
                        generateArtifacts()
                        uploadArtifactsToNexus()
                    }
                }
            }

            // ---------------------------------------------------------
            // DEPLOY STAGE
            // ---------------------------------------------------------
            stage('Deploy to ### Dev Environment') {
                when { expression { params.DEPLOY_TO_ENV } }
                steps {
                    script {
                        echo "=== DEPLOY STAGE ==="
                        runDeployment()
                    }
                }
            }

            // ---------------------------------------------------------
            // CLEANUP STAGE
            // ---------------------------------------------------------
            stage('Cleanup') {
                steps {
                    script {
                        echo "=== CLEANUP STAGE ==="
                        cleanUpReleaseWorkspace()
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
