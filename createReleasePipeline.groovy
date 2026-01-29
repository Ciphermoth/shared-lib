def call(Map config = [:]) {

    pipeline {
        agent any

        parameters {
            booleanParam(
                name: 'IS_CANDIDATE',
                defaultValue: true,
                description: 'Create a release-candidate or official release.'
            )

            booleanParam(
                name: 'DEPLOY_TO_ENV',
                defaultValue: false,
                description: 'Optionally deploy to Dev environment.'
            )
        }

        stages {

            stage('Checkout All Repositories') {
                steps {
                    echo "Checkout stage"
                    // Add your checkout logic here
                }
            }

            stage('Load release-version.json') {
                steps {
                    echo "Load release version info"
                    // Add logic to read version file
                }
            }

            stage('Create Release Candidate Branch') {
                when { expression { params.IS_CANDIDATE } }
                steps {
                    echo "Create release candidate branch"
                    // Add logic for release candidate creation
                }
            }

            stage('Prepare Official Release Branch') {
                when { expression { !params.IS_CANDIDATE } }
                steps {
                    echo "Prepare official release branch"
                    // Add logic for official release prep
                }
            }

            stage('Generate Release Metadata Files') {
                when { expression { !params.IS_CANDIDATE } }
                steps {
                    echo "Generate metadata files"
                    // Add metadata file generation logic
                }
            }

            stage('Pull Release Branch') {
                steps {
                    echo "Pull release branch"
                    // Add logic to pull branch artifacts
                }
            }

            stage('Generate Release Notes') {
                steps {
                    echo "Generate release notes"
                    // Add logic to create release notes
                }
            }

            stage('Nexus Artifact Publishing') {
                steps {
                    echo "Publish artifacts to Nexus"
                    // Add logic to publish artifacts
                }
            }

            stage('Deploy to Dev Environment') {
                when { expression { params.DEPLOY_TO_ENV } }
                steps {
                    echo "Deploy to Dev environment"
                    // Add deployment logic
                }
            }

            stage('Cleanup') {
                steps {
                    script {
                        // Call the shared library function to clean workspace
                        deleteWorkS()
                    }
                }
            }

        }

        post {
            success {
                echo "Release pipeline completed successfully"
            }
            failure {
                echo "Release pipeline failed"
            }
        }
    }
}
