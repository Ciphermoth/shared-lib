
def call(String environment) {
    echo "Deploying to ${environment}..."
    sh """
        ./scripts/deploy.sh ${environment}
    """
}
