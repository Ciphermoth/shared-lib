
def call() {
    echo "Running SVD validation..."
    sh """
        ./scripts/svd-validation.sh
    """
}
