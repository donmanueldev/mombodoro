package dev.momotombo.app.mombodoro

import dev.momotombo.app.mombodoro.data.PomodoroSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.nio.file.Path
import kotlin.io.path.exists
import java.util.concurrent.atomic.AtomicBoolean

enum class MacMenuBarAction {
    Show,
    Hide,
    Exit,
    NotificationOpened,
    NotificationPermissionGranted,
    NotificationPermissionDenied,
    NotificationDeliveryFailed,
    NotificationTestDelivered,
    NotificationTestDenied,
    NotificationTestFailed,
    HostFailed,
}

enum class MacNotificationStatus { Checking, Enabled, Disabled }

enum class MacNotificationTestStatus { Idle, Testing, Delivered, Denied, Failed }

/** Line protocol shared with the native macOS status-bar host. */
internal object MacMenuBarProtocol {
    fun actionFrom(value: String): MacMenuBarAction? =
        MacMenuBarAction.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }

    fun timerCommand(session: PomodoroSession?): String = listOf(
        "timer",
        (session?.let(::timerTitle) ?: "--:--").encode(),
    ).joinToString("\t")

    fun notificationCommand(notification: TimerNotification): String = listOf(
        "notification",
        notification.title.encode(),
        notification.message.encode(),
    ).joinToString("\t")

    fun testNotificationCommand(): String = listOf(
        "testNotification",
        "Prueba de Mombodoro".encode(),
        "Las notificaciones nativas funcionan correctamente.".encode(),
    ).joinToString("\t")

    private fun String.encode(): String = Base64.getEncoder().encodeToString(toByteArray())

    private fun timerTitle(session: PomodoroSession): String = "%02d:%02d".format(
        session.remainingSeconds / 60,
        session.remainingSeconds % 60,
    )
}

class MacMenuBarHost private constructor(private val process: Process) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val actionsChannel = Channel<MacMenuBarAction>(Channel.BUFFERED)
    private val writer = process.outputStream.bufferedWriter()
    private val isClosing = AtomicBoolean(false)

    val actions: Flow<MacMenuBarAction> = actionsChannel.receiveAsFlow()

    init {
        scope.launch {
            try {
                process.inputStream.bufferedReader().forEachLine { line ->
                    MacMenuBarProtocol.actionFrom(line)
                        ?.let(actionsChannel::trySend)
                }
            } finally {
                if (!isClosing.get()) actionsChannel.trySend(MacMenuBarAction.HostFailed)
            }
        }
        scope.launch {
            process.errorStream.bufferedReader().forEachLine { line ->
                System.err.println("Mombodoro notification host: $line")
            }
        }
    }

    fun update(session: PomodoroSession?) {
        send(MacMenuBarProtocol.timerCommand(session))
    }

    fun notify(notification: TimerNotification) {
        send(MacMenuBarProtocol.notificationCommand(notification))
    }

    fun testNotification() {
        send(MacMenuBarProtocol.testNotificationCommand())
    }

    fun openNotificationSettings() {
        send("openNotificationSettings")
    }

    fun close() {
        isClosing.set(true)
        runCatching { writer.close() }
        terminate(process.toHandle())
        scope.cancel()
        actionsChannel.close()
    }

    private fun send(command: String) {
        try {
            writer.write(command)
            writer.newLine()
            writer.flush()
        } catch (_: Exception) {
            actionsChannel.trySend(MacMenuBarAction.HostFailed)
        }
    }

    companion object {
        fun start(): Result<MacMenuBarHost> {
            val executable = MacMenuBarHostResources.executable()
                ?: return Result.failure(IllegalStateException("The packaged macOS notification host is unavailable."))
            return runCatching {
                terminatePreviousHosts(executable)
                val process = ProcessBuilder(executable).apply {
                    System.getProperty("mombodoro.statusIcon")?.let { path ->
                        environment()["MOMBODORO_STATUS_ICON"] = path
                    }
                }.start()
                MacMenuBarHost(process)
            }
        }

        /**
         * El host de barra es un proceso separado del JVM. Si este termina abruptamente,
         * el host no recibe la composición de cierre y queda visible en macOS. Al iniciar,
         * eliminamos únicamente procesos del mismo binario para mantener un solo indicador.
         */
        private fun terminatePreviousHosts(executable: String) {
            ProcessHandle.allProcesses()
                .filter { handle ->
                    handle.pid() != ProcessHandle.current().pid() &&
                        handle.info().command().orElse(null) == executable
                }
                .forEach(::terminate)
        }

        private fun terminate(handle: ProcessHandle) {
            if (!handle.isAlive) return
            handle.destroy()
            val stopped = runCatching {
                handle.onExit().get(1, TimeUnit.SECONDS)
                true
            }.getOrDefault(false)
            if (!stopped && handle.isAlive) handle.destroyForcibly()
        }
    }
}

private object MacMenuBarHostResources {
    private const val applicationResourcesProperty = "compose.application.resources.dir"
    private const val executableName = "MombodoroNotificationHost"

    fun executable(): String? {
        val resources = System.getProperty(applicationResourcesProperty) ?: return null
        val executable = Path.of(resources)
            .resolve("MombodoroNotificationHost.app/Contents/MacOS/$executableName")

        return executable.takeIf { it.exists() && it.toFile().canExecute() }?.toString()
    }
}
