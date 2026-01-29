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
          
        """

        return
    }

    if (isGradle) {
        echo "Detected Gradle project"

        // --- Gradle build logic (skeleton) ---
        sh """
             echo "Gradle build skeleton running"
            # Gradle build commands
         
        """

        return
    }

    error "Unknown build type"
}
