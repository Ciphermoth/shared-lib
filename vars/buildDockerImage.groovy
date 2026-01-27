
def call() {
    echo "Building Docker image..."
    sh """
        docker build -t myapp:${env.BUILD_NUMBER} .
    """
}
