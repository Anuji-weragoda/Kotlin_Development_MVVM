pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Flutter plugin repository
        maven { url = uri("https://storage.googleapis.com/download.flutter.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Local Flutter module AAR repository so app dependencies can resolve
        maven { url = uri("C:/Users/AnujiWeragoda/Android_Project/Kotlin_Development_MVVM/flutter_module/build/host/outputs/repo") }
        // Flutter plugin repository
        maven { url = uri("https://storage.googleapis.com/download.flutter.io") }
    }
}

rootProject.name = "AndroidApp"
include(":app")

// Apply Flutter Gradle script
val flutterModule = file("flutter_module/.android/include_flutter.groovy")
if (flutterModule.exists()) {
    apply(from = flutterModule)
} else {
    println("WARNING: Flutter module include_flutter.groovy not found at $flutterModule")
}
