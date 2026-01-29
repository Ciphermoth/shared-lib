def call() {

    echo "Building application..."

    boolean isCpp = fileExists('CMakeLists.txt')
    boolean isGradle = fileExists('build.gradle') || fileExists('build.gradle.kts')

    if (isCpp) {
        echo "Detected C++ project"

        // --- C++ build logic (skeleton) ---
        sh """
            echo "C++ build skeleton running"
            # C++ build commands 
            # Example:
            # cmake -S . -B build
            # cmake --build build
        """

        return
    }

    if (isGradle) {
        echo "Detected Gradle project"

        // --- Gradle build logic (skeleton) ---
        sh """
             echo "Gradle build skeleton running"
            # Gradle build commands
            # Example:
            # ./gradlew clean build
        """

        return
    }

    error "Unknown build type"
}
