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

enum class MacMenuBarAction {
    Show,
    Hide,
    Exit,
    NotificationOpened,
    NotificationPermissionGranted,
    NotificationPermissionDenied,
    NotificationDeliveryFailed,
}

enum class MacNotificationStatus { Checking, Enabled, Disabled }

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

    val actions: Flow<MacMenuBarAction> = actionsChannel.receiveAsFlow()

    init {
        scope.launch {
            process.inputStream.bufferedReader().forEachLine { line ->
                MacMenuBarProtocol.actionFrom(line)
                    ?.let(actionsChannel::trySend)
            }
        }
    }

    fun update(session: PomodoroSession?) {
        send(MacMenuBarProtocol.timerCommand(session))
    }

    fun notify(notification: TimerNotification) {
        send(MacMenuBarProtocol.notificationCommand(notification))
    }

    fun openNotificationSettings() {
        send("openNotificationSettings")
    }

    fun close() {
        runCatching { writer.close() }
        terminate(process.toHandle())
        scope.cancel()
        actionsChannel.close()
    }

    private fun send(command: String) {
        runCatching {
            writer.write(command)
            writer.newLine()
            writer.flush()
        }
    }

    companion object {
        fun start(): MacMenuBarHost? {
            val executable = MacMenuBarHostResources.executable() ?: return null
            return runCatching {
                terminatePreviousHosts(executable)
                val process = ProcessBuilder(executable).apply {
                    System.getProperty("mombodoro.statusIcon")?.let { path ->
                        environment()["MOMBODORO_STATUS_ICON"] = path
                    }
                }.start()
                MacMenuBarHost(process)
            }.getOrNull()
        }

        /**
         * El host de barra es un proceso separado del JVM. Si este termina abruptamente,
         * el host no recibe la composición de cierre y queda visible en macOS. Al iniciar,
         * eliminamos únicamente procesos del mismo binario para mantener un solo indicador.
         */
        private fun terminatePreviousHosts(executable: String) {
            runCatching {
                ProcessHandle.allProcesses()
                    .filter { handle ->
                        handle.pid() != ProcessHandle.current().pid() &&
                            handle.info().command().orElse(null) == executable
                    }
                    .forEach(::terminate)
            }
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
