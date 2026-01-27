def call() {

    echo "Building application..."

    // Detecting project type
    boolean isCpp = fileExists('CMakeLists.txt')
    boolean isGradle = fileExists('build.gradle') || fileExists('build.gradle.kts')

    if (isCpp) {
        echo "Detected C++ project (CMakeLists.txt found)"
        sh """
            mkdir -p build
            cd build
            cmake ..
            cmake --build .
        """
        return
    }

    if (isGradle) {
        echo "Detected Java Gradle project (build.gradle found)"
        sh """
            ./gradlew clean build
        """
        return
    }

    // project type is unknown
    error """
    Unable to determine project type.
    Expected one of:
      - CMakeLists.txt (C++)
      - build.gradle or build.gradle.kts (Java/Gradle)
    """
}
