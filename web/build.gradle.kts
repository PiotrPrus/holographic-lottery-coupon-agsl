import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

// Web demos for the talk: the audience opens them from a QR code and scratches on their own phone.
//
//   ./gradlew :web:wasmJsBrowserDevelopmentRun     dev server with live reload
//   ./gradlew :web:wasmJsBrowserDistribution       release bundle → web/build/dist/wasmJs/productionExecutable
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("holoscratch")
        browser {
            commonWebpackConfig {
                outputFileName = "holoscratch.js"
                // No source map in the published bundle.
                devtool = if (mode == org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.PRODUCTION) null else devtool
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(libs.compose.mp.runtime)
            implementation(libs.compose.mp.foundation)
            implementation(libs.compose.mp.ui)
            implementation(libs.particle.emitter) // dust on the Final page
        }
    }
}

// settings.gradle.kts uses FAIL_ON_PROJECT_REPOS, so Kotlin must not add its own download
// repositories for Node.js, Yarn and Binaryen. Clearing downloadBaseUrl makes it resolve them from
// the ivy repositories declared in settings.gradle.kts instead.
@OptIn(ExperimentalWasmDsl::class)
listOf(project, rootProject).forEach { p ->
    p.plugins.withType<WasmNodeJsPlugin> { p.the<WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?) }
    p.plugins.withType<WasmNodeJsRootPlugin> { p.the<WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?) }
    p.plugins.withType<WasmYarnPlugin> { p.the<WasmYarnRootEnvSpec>().downloadBaseUrl.set(null as String?) }
    p.plugins.withType<BinaryenPlugin> { p.the<BinaryenEnvSpec>().downloadBaseUrl.set(null as String?) }
}

// The root package.json is named after rootProject.name ("Holo Scratch"), and npm/yarn reject the
// space. Rename it in the generated file rather than renaming the whole Gradle project.
val rootPackageJson = rootProject.layout.buildDirectory.file("wasm/package.json")
rootProject.tasks.matching { it.name == "wasmRootPackageJson" }.configureEach {
    doLast {
        val file = rootPackageJson.get().asFile
        file.writeText(file.readText().replace("\"name\": \"Holo Scratch\"", "\"name\": \"holo-scratch\""))
    }
}
