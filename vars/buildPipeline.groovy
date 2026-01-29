def call() {

    echo "Building application..."

    boolean isCpp = fileExists('CMakeLists.txt')
    boolean isGradle = fileExists('build.gradle') || fileExists('build.gradle.kts')

    if (isCpp) {
        echo "Detected C++ project"
        echo "C++ build would run here"
        return
    }

    if (isGradle) {
        echo "Detected Gradle project"
        echo "Gradle build would run here"
        return
    }

    error "Unknown project type"
}
