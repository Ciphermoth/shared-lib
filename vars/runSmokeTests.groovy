
def call() {
    echo "Running smoke tests..."
    sh """
        ./scripts/smoke-tests.sh
    """
}
