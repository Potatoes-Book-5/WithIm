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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Pleos Connect SDK (Vehicle, NaviHelper, TextToSpeech, SpeechToText, FusedLocation)
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
        // 네이버 지도 SDK (ADR-004)
        maven("https://repository.map.naver.com/archive/maven")
    }
}

rootProject.name = "King-of-real-estate"
include(":app")
include(":core:domain", ":core:data", ":feature:yeongtong")
include(":core:designsystem", ":core:ui", ":feature:driving")
