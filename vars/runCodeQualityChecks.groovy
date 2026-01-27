
def call() {
    echo "Running code quality checks..."
    sh """
        ./gradlew check
    """
}
