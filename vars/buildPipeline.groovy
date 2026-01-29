def call() {

    echo "Building application..."

    boolean isCpp = fileExists('CMakeLists.txt')
    boolean isGradle = fileExists('build.gradle') || fileExists('build.gradle.kts')

    if (isCpp) {
        echo "Detected C++ project"
        echo "C++ build runs here"
        return
    }

    if (isGradle) {
        echo "Detected Gradle project"
        echo "Gradle buildd runs here"
        return
    }

    error "Unknown project type"
}
