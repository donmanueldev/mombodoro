import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.ExperimentalComposeLibrary

val macMenuBarHostSource = layout.projectDirectory.file("src/desktopMain/native/macos/MenuBarHost.swift")
val macMenuBarProtocolSource = layout.projectDirectory.file("src/desktopMain/native/macos/MenuBarProtocol.swift")
val macMenuBarHostBinary = layout.buildDirectory.file("MombodoroMenuBarHost")

val compileMacMenuBarHost by tasks.registering(Exec::class) {
    onlyIf { System.getProperty("os.name") == "Mac OS X" }
    inputs.file(macMenuBarHostSource)
    inputs.file(macMenuBarProtocolSource)
    outputs.file(macMenuBarHostBinary)
    commandLine(
        "xcrun",
        "swiftc",
        macMenuBarHostSource.asFile.absolutePath,
        macMenuBarProtocolSource.asFile.absolutePath,
        "-framework",
        "Cocoa",
        "-o",
        macMenuBarHostBinary.get().asFile.absolutePath,
    )
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

@OptIn(ExperimentalComposeLibrary::class)
kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqlite.jdbc)
        }
        desktopTest.dependencies {
            implementation(compose.uiTest)
        }
    }
}


compose.desktop {
    application {
        mainClass = "dev.momotombo.app.mombodoro.MainKt"

        if (System.getProperty("os.name") == "Mac OS X") {
            jvmArgs += listOf(
                "-Xdock:name=Mombodoro",
                "-Xdock:icon=${project.file("src/desktopMain/resources/Mombo.icns").absolutePath}",
                "-Dmombodoro.menuHost=${macMenuBarHostBinary.get().asFile.absolutePath}",
                "-Dmombodoro.statusIcon=${project.file("src/desktopMain/composeResources/drawable/mombo_status_icon.svg").absolutePath}",
            )
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Mombodoro"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("src/desktopMain/resources/Mombo.icns"))
                dockName = "Mombodoro"
                bundleID = "dev.momotombo.Mombodoro"
            }
        }
    }
}

tasks.configureEach {
    if (name == "run") {
        dependsOn(compileMacMenuBarHost)
    }
}
