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
                }
            }

            stage('Load release-version.json') {
                steps {
                    echo "Load release version info"
                }
            }

            stage('Create Release Candidate Branch') {
                when { expression { params.IS_CANDIDATE } }
                steps {
                    echo "Create release candidate branch"
                }
            }

            stage('Prepare Official Release Branch') {
                when { expression { !params.IS_CANDIDATE } }
                steps {
                    echo "Prepare official release branch"
                }
            }

            stage('Generate Release Metadata Files') {
                when { expression { !params.IS_CANDIDATE } }
                steps {
                    echo "Generate metadata files"
                }
            }

            stage('Pull Release Branch') {
                steps {
                    echo "Pull release branch"
                }
            }

            stage('Generate Release Notes') {
                steps {
                    echo "Generate release notes"
                }
            }

            stage('Nexus Artifact Publishing') {
                steps {
                    echo "Publish artifacts to Nexus"
                }
            }

            stage('Deploy to Dev Environment') {
                when { expression { params.DEPLOY_TO_ENV } }
                steps {
                    echo "Deploy to Dev environment"
                }
            }

            stage('Cleanup') {
                steps {
                    echo "Cleanup workspace"
                }
            }
        }

        post {
            success { echo "Release pipeline completed successfully" }
            failure { echo "Release pipeline failed" }
        }
    }
}
