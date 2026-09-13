pluginManagement {
    repositories {
        maven { url = java.net.URI("https://maven.aliyun.com/repository/public") }
        maven { url = java.net.URI("https://maven.aliyun.com/repository/google") }
        maven { url = java.net.URI("https://mirrors.cloud.tencent.com/nexus/repository/maven-public") }
        maven { url = java.net.URI("https://repo.huaweicloud.com/repository/maven") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = java.net.URI("https://maven.aliyun.com/repository/google") }
        maven { url = java.net.URI("https://maven.aliyun.com/repository/public") }
        maven { url = java.net.URI("https://mirrors.cloud.tencent.com/nexus/repository/maven-public") }
        maven { url = java.net.URI("https://repo.huaweicloud.com/repository/maven") }
        google()
        mavenCentral()
    }
}

rootProject.name = "FloatingClock"
include(":app")
