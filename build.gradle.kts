plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

subprojects {
    plugins.withId("com.android.application") {
        dependencies.add("implementation", "org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")
    }
}

allprojects {
    tasks.register("resolveAllDependencies") {
        group = "dependency"
        description = "Pre-fetches all dependency artifacts for offline Codex agent execution."
        doLast {
            configurations
                .filter { it.isCanBeResolved }
                .forEach { config ->
                    println("Resolving ${config.name} in ${project.name}")
                    config.resolve()
                }
        }
    }
}
