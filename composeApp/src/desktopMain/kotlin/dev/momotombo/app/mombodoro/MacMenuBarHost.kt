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
import java.io.BufferedWriter
import java.util.Base64
import java.util.concurrent.TimeUnit

enum class MacMenuBarAction { Toggle, Show, Exit }

class MacMenuBarHost private constructor(private val process: Process) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val actionsChannel = Channel<MacMenuBarAction>(Channel.BUFFERED)
    private val writer = process.outputStream.bufferedWriter()

    val actions: Flow<MacMenuBarAction> = actionsChannel.receiveAsFlow()

    init {
        scope.launch {
            process.inputStream.bufferedReader().forEachLine { line ->
                MacMenuBarAction.entries.firstOrNull { it.name.equals(line, ignoreCase = true) }
                    ?.let(actionsChannel::trySend)
            }
        }
    }

    fun update(session: PomodoroSession?) {
        val title = session?.let(::statusTitle) ?: "Mombodoro"
        send("state\t${title.encode()}\t${if (session?.isRunning == true) 1 else 0}")
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

    private fun statusTitle(session: PomodoroSession): String = "%02d:%02d · %s%s".format(
        session.remainingSeconds / 60,
        session.remainingSeconds % 60,
        session.phaseTitle,
        if (session.isRunning) "" else " · Pausado",
    )

    private fun String.encode(): String = Base64.getEncoder().encodeToString(toByteArray())

    companion object {
        fun start(): MacMenuBarHost? {
            val executable = System.getProperty("mombodoro.menuHost") ?: return null
            return runCatching {
                terminatePreviousHosts(executable)
                MacMenuBarHost(ProcessBuilder(executable).start())
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
