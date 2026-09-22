import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.ExperimentalComposeLibrary

val isMacOS = System.getProperty("os.name") == "Mac OS X"
val macMinimumSystemVersion = "12.0"
val macArchitecture = when (System.getProperty("os.arch")) {
    "aarch64" -> "arm64"
    "x86_64", "amd64" -> "x86_64"
    else -> error("Unsupported macOS architecture: ${System.getProperty("os.arch")}")
}
val macSigningIdentity = providers.gradleProperty("compose.desktop.mac.signing.identity")
val effectiveMacSigningIdentity = macSigningIdentity.orElse("-")
val macAppStore = providers.gradleProperty("mombodoro.appStore").map(String::toBoolean).orElse(false)
val macProvisioningProfile = providers.gradleProperty("mombodoro.provisioningProfile").orElse(
    providers.systemProperty("user.home").map { "$it/Downloads/Mombodoro_Mac_App_Store.provisionprofile" }
)
val macAppStoreApplicationIdentity = providers.gradleProperty("mombodoro.appStoreApplicationIdentity")
    .orElse("Apple Distribution: Manuel Duarte (2XUFV8QBCV)")
val macAppStoreInstallerIdentity = providers.gradleProperty("mombodoro.appStoreInstallerIdentity")
    .orElse("3rd Party Mac Developer Installer: Manuel Duarte (2XUFV8QBCV)")
val applicationVersion = providers.gradleProperty("mombodoro.version").orElse("1.0.0")
val applicationBuildNumber = providers.gradleProperty("mombodoro.buildNumber").orElse("1")

fun codesignCommand(
    identity: String,
    target: String,
    preserveExistingMetadata: Boolean = false,
): List<String> = buildList {
    addAll(listOf("codesign", "--force", "--deep"))
    if (identity == "-") {
        add("--timestamp=none")
    } else {
        addAll(listOf("--options", "runtime", "--timestamp"))
        if (preserveExistingMetadata) {
            add("--preserve-metadata=entitlements,requirements,flags,runtime")
        }
    }
    addAll(listOf("--sign", identity, target))
}

fun appStoreCodesignCommand(target: String, entitlements: String): List<String> = listOf(
    "codesign", "--force", "--options", "runtime", "--timestamp",
    "--entitlements", entitlements,
    "--sign", macAppStoreApplicationIdentity.get(),
    target,
)

val macMenuBarHostSource = layout.projectDirectory.file("src/desktopMain/native/macos/MenuBarHost.swift")
val macMenuBarProtocolSource = layout.projectDirectory.file("src/desktopMain/native/macos/MenuBarProtocol.swift")
val macMenuBarHostInfo = layout.projectDirectory.file("src/desktopMain/native/macos/MombodoroNotificationHost-Info.plist")
val macMenuBarHostResources = layout.buildDirectory.dir("generated/mombodoroMenuHostResources")
val macMenuBarHostExecutable = layout.buildDirectory.file("MombodoroNotificationHost")
val macMenuBarProtocolTestSource = layout.projectDirectory.file("src/desktopTest/native/macos/MenuBarProtocolTests.swift")
val macMenuBarProtocolTestExecutable = layout.buildDirectory.file("MombodoroMenuBarProtocolTests")
val stagedMacProvisioningProfile = layout.buildDirectory.file("generated/appStore/embedded.provisionprofile")

val stageMacProvisioningProfile by tasks.registering(Copy::class) {
    onlyIf { macAppStore.get() }
    from(macProvisioningProfile)
    into(stagedMacProvisioningProfile.map { it.asFile.parentFile })
    rename { stagedMacProvisioningProfile.get().asFile.name }
    outputs.file(stagedMacProvisioningProfile)
}

val compileMacMenuBarHost by tasks.registering(Exec::class) {
    notCompatibleWithConfigurationCache("Invokes the platform Swift toolchain.")
    onlyIf { isMacOS }
    inputs.file(macMenuBarHostSource)
    inputs.file(macMenuBarProtocolSource)
    inputs.property("minimumSystemVersion", macMinimumSystemVersion)
    inputs.property("architecture", macArchitecture)
    outputs.file(macMenuBarHostExecutable)
    commandLine(
        "xcrun",
        "swiftc",
        macMenuBarHostSource.asFile.absolutePath,
        macMenuBarProtocolSource.asFile.absolutePath,
        "-target",
        "$macArchitecture-apple-macosx$macMinimumSystemVersion",
        "-framework",
        "Cocoa",
        "-framework",
        "UserNotifications",
        "-o",
        macMenuBarHostExecutable.get().asFile.absolutePath,
    )
}

val compileMacMenuBarProtocolTests by tasks.registering(Exec::class) {
    notCompatibleWithConfigurationCache("Invokes the platform Swift toolchain.")
    onlyIf { isMacOS }
    inputs.file(macMenuBarProtocolSource)
    inputs.file(macMenuBarProtocolTestSource)
    inputs.property("minimumSystemVersion", macMinimumSystemVersion)
    inputs.property("architecture", macArchitecture)
    outputs.file(macMenuBarProtocolTestExecutable)
    commandLine(
        "xcrun",
        "swiftc",
        macMenuBarProtocolSource.asFile.absolutePath,
        macMenuBarProtocolTestSource.asFile.absolutePath,
        "-target",
        "$macArchitecture-apple-macosx$macMinimumSystemVersion",
        "-o",
        macMenuBarProtocolTestExecutable.get().asFile.absolutePath,
    )
}

val macMenuBarProtocolTest by tasks.registering(Exec::class) {
    notCompatibleWithConfigurationCache("Runs a native macOS test executable.")
    onlyIf { isMacOS }
    dependsOn(compileMacMenuBarProtocolTests)
    commandLine(macMenuBarProtocolTestExecutable.get().asFile.absolutePath)
}

val assembleMacMenuBarHost by tasks.registering(Sync::class) {
    notCompatibleWithConfigurationCache("Stages a generated native macOS application bundle.")
    dependsOn(compileMacMenuBarHost)
    inputs.file(macMenuBarHostInfo)
    inputs.property("applicationVersion", applicationVersion)
    inputs.property("applicationBuildNumber", applicationBuildNumber)
    from(macMenuBarHostInfo) {
        into("macos/MombodoroNotificationHost.app/Contents")
        rename { "Info.plist" }
        filter { line ->
            line.replace("@APP_VERSION@", applicationVersion.get())
                .replace("@BUILD_NUMBER@", applicationBuildNumber.get())
                .replace("@MINIMUM_SYSTEM_VERSION@", macMinimumSystemVersion)
        }
    }
    from(macMenuBarHostExecutable) {
        into("macos/MombodoroNotificationHost.app/Contents/MacOS")
    }
    into(macMenuBarHostResources)
}

val signMacMenuBarHost by tasks.registering(Exec::class) {
    notCompatibleWithConfigurationCache("Signs a generated macOS application bundle.")
    onlyIf { isMacOS }
    dependsOn(assembleMacMenuBarHost)
    val hostBundle = macMenuBarHostResources.map { directory ->
        directory.dir("macos/MombodoroNotificationHost.app")
    }
    inputs.dir(hostBundle)
    inputs.property("signingIdentity", effectiveMacSigningIdentity)
    outputs.dir(hostBundle)
    commandLine(
        codesignCommand(
            identity = effectiveMacSigningIdentity.get(),
            target = hostBundle.get().asFile.absolutePath,
        )
    )
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

@OptIn(ExperimentalComposeLibrary::class)
kotlin {
    jvmToolchain(21)
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
        jvmArgs += "-Dmombodoro.version=${applicationVersion.get()}"

        if (isMacOS) {
            jvmArgs += listOf(
                "-Xdock:name=Mombodoro",
                "-Xdock:icon=${project.file("src/desktopMain/resources/Mombo.icns").absolutePath}",
                "-Dmombodoro.statusIcon=${project.file("src/desktopMain/composeResources/drawable/mombo_status_icon.svg").absolutePath}",
            )
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            modules("java.sql")
            packageName = "Mombodoro"
            packageVersion = applicationVersion.get()

            macOS {
                iconFile.set(project.file("src/desktopMain/resources/Mombo.icns"))
                dockName = "Mombodoro"
                bundleID = "dev.momotombo.mombodoro"
                minimumSystemVersion = macMinimumSystemVersion
                packageBuildVersion = applicationBuildNumber.get()
                appCategory = "public.app-category.productivity"
                appStore = macAppStore.get()
                if (macAppStore.get()) {
                    entitlementsFile.set(project.file("src/desktopMain/native/macos/Mombodoro.entitlements"))
                    runtimeEntitlementsFile.set(project.file("src/desktopMain/native/macos/MombodoroRuntime.entitlements"))
                    provisioningProfile.set(stagedMacProvisioningProfile)
                }
                signing {
                    sign.set(macSigningIdentity.isPresent)
                    identity.set(macSigningIdentity)
                }
            }
            if (isMacOS) {
                appResourcesRootDir.set(macMenuBarHostResources)
            }
        }
    }
}

tasks.configureEach {
    if (isMacOS && (name == "run" || name == "prepareAppResources")) {
        dependsOn(signMacMenuBarHost)
    }
    if (isMacOS && name == "createDistributable") {
        dependsOn(stageMacProvisioningProfile)
    }
}

val makePackagedMacNotificationHostExecutable by tasks.registering(Exec::class) {
    notCompatibleWithConfigurationCache("Changes permissions in a generated macOS application bundle.")
    onlyIf { isMacOS }
    dependsOn("createDistributable")
    val hostExecutable = layout.buildDirectory.file(
        "compose/binaries/main/app/Mombodoro.app/Contents/app/resources/MombodoroNotificationHost.app/Contents/MacOS/MombodoroNotificationHost"
    )
    commandLine("chmod", "+x", hostExecutable.get().asFile.absolutePath)
}

val signPackagedMacNotificationHost by tasks.registering(Exec::class) {
    notCompatibleWithConfigurationCache("Signs a generated macOS application bundle.")
    onlyIf { isMacOS }
    dependsOn(makePackagedMacNotificationHostExecutable)
    val hostBundle = layout.buildDirectory.dir(
        "compose/binaries/main/app/Mombodoro.app/Contents/app/resources/MombodoroNotificationHost.app"
    )
    commandLine(
        codesignCommand(
            identity = effectiveMacSigningIdentity.get(),
            target = hostBundle.get().asFile.absolutePath,
        )
    )
}

val finalizeMacNotificationHostBundle by tasks.registering(Exec::class) {
    notCompatibleWithConfigurationCache("Signs a generated macOS application bundle.")
    onlyIf { isMacOS }
    dependsOn(signPackagedMacNotificationHost)
    val applicationBundle = layout.buildDirectory.dir("compose/binaries/main/app/Mombodoro.app")
    commandLine(
        codesignCommand(
            identity = effectiveMacSigningIdentity.get(),
            target = applicationBundle.get().asFile.absolutePath,
            preserveExistingMetadata = macSigningIdentity.isPresent,
        )
    )
}

if (isMacOS) {
    tasks.configureEach {
        if (name == "createDistributable") {
            finalizedBy(finalizeMacNotificationHostBundle)
        }
    }
}

val packagedMacApplication = layout.buildDirectory.dir("compose/binaries/main/app/Mombodoro.app")
val packagedMacRuntime = packagedMacApplication.map { it.dir("Contents/runtime") }
val packagedMacSkiko = packagedMacApplication.map { it.file("Contents/app/libskiko-macos-arm64.dylib") }
val packagedMacNotificationHost = packagedMacApplication.map {
    it.dir("Contents/app/resources/MombodoroNotificationHost.app")
}
val appStoreEntitlements = layout.projectDirectory.file("src/desktopMain/native/macos/Mombodoro.entitlements")
val appStoreRuntimeEntitlements =
    layout.projectDirectory.file("src/desktopMain/native/macos/MombodoroRuntime.entitlements")
val appStorePkg = layout.buildDirectory.file(
    applicationVersion.map { "compose/binaries/main/pkg/Mombodoro-$it.pkg" }
)

fun requireAppStoreBuild() {
    check(macAppStore.get()) {
        "Use -Pmombodoro.appStore=true when generating the Mac App Store package."
    }
}

val signAppStoreRuntimeFiles by tasks.registering(Exec::class) {
    dependsOn(finalizeMacNotificationHostBundle)
    doFirst { requireAppStoreBuild() }
    commandLine(
        "find",
        packagedMacRuntime.get().asFile.absolutePath,
        "-type", "f",
        "(", "-perm", "-111", "-o", "-name", "*.dylib", "-o", "-name", "*.jnilib", ")",
        "-exec",
        "codesign", "--force", "--options", "runtime", "--timestamp",
        "--entitlements", appStoreRuntimeEntitlements.asFile.absolutePath,
        "--sign", macAppStoreApplicationIdentity.get(),
        "{}", ";",
    )
}

val signAppStoreRuntimeBundle by tasks.registering(Exec::class) {
    dependsOn(signAppStoreRuntimeFiles)
    commandLine(appStoreCodesignCommand(
        packagedMacRuntime.get().asFile.absolutePath,
        appStoreRuntimeEntitlements.asFile.absolutePath,
    ))
}

val signAppStoreSkiko by tasks.registering(Exec::class) {
    dependsOn(signAppStoreRuntimeBundle)
    commandLine(appStoreCodesignCommand(
        packagedMacSkiko.get().asFile.absolutePath,
        appStoreRuntimeEntitlements.asFile.absolutePath,
    ))
}

val signAppStoreNotificationHost by tasks.registering(Exec::class) {
    dependsOn(signAppStoreSkiko)
    commandLine(appStoreCodesignCommand(
        packagedMacNotificationHost.get().asFile.absolutePath,
        appStoreRuntimeEntitlements.asFile.absolutePath,
    ))
}

val signAppStoreApplication by tasks.registering(Exec::class) {
    dependsOn(signAppStoreNotificationHost)
    commandLine(appStoreCodesignCommand(
        packagedMacApplication.get().asFile.absolutePath,
        appStoreEntitlements.asFile.absolutePath,
    ))
}

val verifyAppStoreApplication by tasks.registering(Exec::class) {
    dependsOn(signAppStoreApplication)
    commandLine(
        "codesign", "--verify", "--deep", "--strict", "--verbose=4",
        packagedMacApplication.get().asFile.absolutePath,
    )
}

val packageAppStorePkg by tasks.registering(Exec::class) {
    dependsOn(verifyAppStoreApplication)
    val outputFile = appStorePkg.get().asFile
    doFirst {
        requireAppStoreBuild()
        outputFile.parentFile.mkdirs()
        outputFile.delete()
    }
    commandLine(
        "productbuild",
        "--component", packagedMacApplication.get().asFile.absolutePath,
        "/Applications",
        "--sign", macAppStoreInstallerIdentity.get(),
        outputFile.absolutePath,
    )
}

val verifyAppStorePkg by tasks.registering(Exec::class) {
    dependsOn(packageAppStorePkg)
    commandLine("pkgutil", "--check-signature", appStorePkg.get().asFile.absolutePath)
}

packageAppStorePkg {
    finalizedBy(verifyAppStorePkg)
}

val verifyMacNotificationHostBundle by tasks.registering(Exec::class) {
    notCompatibleWithConfigurationCache("Inspects a generated macOS application bundle with native tools.")
    onlyIf { isMacOS }
    dependsOn(finalizeMacNotificationHostBundle)
    val hostInfo = layout.buildDirectory.file(
        "compose/binaries/main/app/Mombodoro.app/Contents/app/resources/MombodoroNotificationHost.app/Contents/Info.plist"
    )
    val hostExecutable = layout.buildDirectory.file(
        "compose/binaries/main/app/Mombodoro.app/Contents/app/resources/MombodoroNotificationHost.app/Contents/MacOS/MombodoroNotificationHost"
    )
    val applicationBundle = layout.buildDirectory.dir("compose/binaries/main/app/Mombodoro.app")
    val runtimeRelease = layout.buildDirectory.file(
        "compose/binaries/main/app/Mombodoro.app/Contents/runtime/Contents/Home/release"
    )
    commandLine(
        "/bin/sh",
        "-c",
        "test -x '${hostExecutable.get().asFile}' && " +
            "test \"\$(/usr/libexec/PlistBuddy -c 'Print :CFBundleDisplayName' '${hostInfo.get().asFile}')\" = Mombodoro && " +
            "test \"\$(/usr/libexec/PlistBuddy -c 'Print :LSMinimumSystemVersion' '${hostInfo.get().asFile}')\" = '$macMinimumSystemVersion' && " +
            "vtool -show-build '${hostExecutable.get().asFile}' | grep -q 'minos $macMinimumSystemVersion' && " +
            "grep -q ' java.sql' '${runtimeRelease.get().asFile}' && " +
            "codesign --verify --deep --strict '${applicationBundle.get().asFile}'",
    )
}
