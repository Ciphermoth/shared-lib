
def call() {
    echo "Running code quality checks..."
    sh """
        ./gradlew check THAT ONE THING WE TRYING TO USE
    """
}
