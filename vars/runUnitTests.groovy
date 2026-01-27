
def call() {
    echo "Running unit tests..."
    sh """
        ./gradlew test
    """
}
