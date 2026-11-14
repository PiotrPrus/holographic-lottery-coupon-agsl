import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

// ParticleEmitter ships Java 21 bytecode, so the deck runs on a 21 toolchain (auto-provisioned
// by foojay) no matter which JDK runs Gradle.
val deckJava = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.mp.material3)
    implementation(libs.composemediaplayer)
    implementation(libs.particle.emitter)
    implementation(libs.qrose)
}

compose.desktop {
    application {
        javaHome = deckJava.get().metadata.installationPath.asFile.absolutePath
        mainClass = "dev.piotrprus.holoscratch.presentation.MainKt"
        // ./gradlew :presentation:run -Pslide=8 -Pwindowed
        args += listOfNotNull(
            (findProperty("slide") as String?)?.let { "--slide=$it" },
            if (hasProperty("windowed")) "--windowed" else null,
        )

        nativeDistributions {
            packageName = "ScratchToRevealTalk"
            packageVersion = "1.0.0"
        }
    }
}

// Dev tool: render slides to PNGs without opening a window.
tasks.register<JavaExec>("renderSlides") {
    group = "presentation"
    javaLauncher.set(deckJava)
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.piotrprus.holoscratch.presentation.tools.RenderSlidesKt")
    args(
        (findProperty("out") as String?) ?: layout.buildDirectory.dir("slides").get().asFile.path,
        (findProperty("slides") as String?) ?: "",
        (findProperty("section") as String?) ?: "",
    )
}
