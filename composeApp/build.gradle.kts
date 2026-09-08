import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.ExperimentalComposeLibrary

val macMenuBarHostSource = layout.projectDirectory.file("src/desktopMain/native/macos/MenuBarHost.swift")
val macMenuBarProtocolSource = layout.projectDirectory.file("src/desktopMain/native/macos/MenuBarProtocol.swift")
val macMenuBarHostInfo = layout.projectDirectory.file("src/desktopMain/native/macos/MombodoroNotificationHost-Info.plist")
val macMenuBarHostResources = layout.buildDirectory.dir("generated/mombodoroMenuHostResources")
val macMenuBarHostExecutable = layout.buildDirectory.file("MombodoroNotificationHost")

val compileMacMenuBarHost by tasks.registering(Exec::class) {
    onlyIf { System.getProperty("os.name") == "Mac OS X" }
    inputs.file(macMenuBarHostSource)
    inputs.file(macMenuBarProtocolSource)
    outputs.file(macMenuBarHostExecutable)
    commandLine(
        "xcrun",
        "swiftc",
        macMenuBarHostSource.asFile.absolutePath,
        macMenuBarProtocolSource.asFile.absolutePath,
        "-framework",
        "Cocoa",
        "-framework",
        "UserNotifications",
        "-o",
        macMenuBarHostExecutable.get().asFile.absolutePath,
    )
}

val assembleMacMenuBarHost by tasks.registering(Sync::class) {
    dependsOn(compileMacMenuBarHost)
    inputs.file(macMenuBarHostInfo)
    from(macMenuBarHostInfo) {
        into("macos/MombodoroNotificationHost.app/Contents")
        rename { "Info.plist" }
    }
    from(macMenuBarHostExecutable) {
        into("macos/MombodoroNotificationHost.app/Contents/MacOS")
    }
    into(macMenuBarHostResources)
}

val signMacMenuBarHost by tasks.registering(Exec::class) {
    dependsOn(assembleMacMenuBarHost)
    val hostBundle = macMenuBarHostResources.map { directory ->
        directory.dir("macos/MombodoroNotificationHost.app")
    }
    inputs.dir(hostBundle)
    outputs.dir(hostBundle)
    commandLine("codesign", "--force", "--deep", "--sign", "-", hostBundle.get().asFile.absolutePath)
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
            appResourcesRootDir.set(macMenuBarHostResources)
        }
    }
}

tasks.configureEach {
    if (name == "run" || name == "prepareAppResources") {
        dependsOn(signMacMenuBarHost)
    }
}

val finalizeMacNotificationHostBundle by tasks.registering(Exec::class) {
    dependsOn("createDistributable")
    val hostExecutable = layout.buildDirectory.file(
        "compose/binaries/main/app/Mombodoro.app/Contents/app/resources/MombodoroNotificationHost.app/Contents/MacOS/MombodoroNotificationHost"
    )
    val applicationBundle = layout.buildDirectory.dir("compose/binaries/main/app/Mombodoro.app")
    commandLine(
        "/bin/sh",
        "-c",
        "chmod +x '${hostExecutable.get().asFile}' && codesign --force --deep --sign - '${applicationBundle.get().asFile}'",
    )
}

tasks.configureEach {
    if (name == "createDistributable") {
        finalizedBy(finalizeMacNotificationHostBundle)
    }
}

val verifyMacNotificationHostBundle by tasks.registering(Exec::class) {
    dependsOn(finalizeMacNotificationHostBundle)
    val hostInfo = layout.buildDirectory.file(
        "compose/binaries/main/app/Mombodoro.app/Contents/app/resources/MombodoroNotificationHost.app/Contents/Info.plist"
    )
    val hostExecutable = layout.buildDirectory.file(
        "compose/binaries/main/app/Mombodoro.app/Contents/app/resources/MombodoroNotificationHost.app/Contents/MacOS/MombodoroNotificationHost"
    )
    val applicationBundle = layout.buildDirectory.dir("compose/binaries/main/app/Mombodoro.app")
    commandLine(
        "/bin/sh",
        "-c",
        "test -x '${hostExecutable.get().asFile}' && test \"\$(/usr/libexec/PlistBuddy -c 'Print :CFBundleDisplayName' '${hostInfo.get().asFile}')\" = Mombodoro && codesign --verify --deep --strict '${applicationBundle.get().asFile}'",
    )
}
