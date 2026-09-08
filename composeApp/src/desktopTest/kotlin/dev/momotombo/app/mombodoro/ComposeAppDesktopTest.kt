package dev.momotombo.app.mombodoro

import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.data.PomodoroSession
import dev.momotombo.app.mombodoro.data.TimerEvent
import dev.momotombo.app.mombodoro.data.TimerSpeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ComposeAppDesktopTest {

    @Test
    fun `a selected focus type starts a new focus session`() {
        val configuration = PomodoroConfiguration(
            name = "Estudio",
            focusSeconds = 50 * 60,
            shortBreakSeconds = 10 * 60,
            longBreakSeconds = 30 * 60,
            cyclesBeforeLongBreak = 3
        )

        val session = PomodoroSession.start(configuration)

        assertEquals(Pomodoro.FOCUS, session.phase)
        assertEquals(50 * 60, session.remainingSeconds)
        assertEquals(0, session.completedFocusSessions)
        assertEquals(false, session.isRunning)
    }

    @Test
    fun `completing a focus period starts a short break and records progress`() {
        val configuration = testConfiguration(cycles = 2)
        val session = PomodoroSession.start(configuration).copy(
            remainingSeconds = 1,
            isRunning = true,
        )

        val result = session.tick()

        assertEquals(Pomodoro.BREAK, result.session.phase)
        assertEquals(configuration.shortBreakSeconds, result.session.remainingSeconds)
        assertEquals(1, result.session.completedFocusSessions)
        assertEquals(false, result.session.isRunning)
        assertIs<TimerEvent.PhaseCompleted>(result.event)
    }

    @Test
    fun `completing the configured number of focus periods starts a long break`() {
        val configuration = testConfiguration(cycles = 2)
        val session = PomodoroSession.start(configuration).copy(
            remainingSeconds = 1,
            completedFocusSessions = 1,
            isRunning = true,
        )

        val result = session.tick()

        assertEquals(Pomodoro.LONG_BREAK, result.session.phase)
        assertEquals(configuration.longBreakSeconds, result.session.remainingSeconds)
        assertEquals(0, result.session.completedFocusSessions)
    }

    @Test
    fun `changing configuration always restarts at focus without prior progress`() {
        val previous = PomodoroSession.start(testConfiguration(cycles = 4)).copy(
            phase = Pomodoro.BREAK,
            remainingSeconds = 120,
            completedFocusSessions = 3,
        )
        val updated = testConfiguration(name = "Trabajo", cycles = 3)

        val restarted = previous.restartWith(updated)

        assertEquals(Pomodoro.FOCUS, restarted.phase)
        assertEquals(updated.focusSeconds, restarted.remainingSeconds)
        assertEquals(0, restarted.completedFocusSessions)
        assertEquals(false, restarted.isRunning)
    }

    @Test
    fun `completing a break returns to focus while keeping progress toward long break`() {
        val configuration = testConfiguration(cycles = 4)
        val session = PomodoroSession.start(configuration).copy(
            phase = Pomodoro.BREAK,
            remainingSeconds = 1,
            completedFocusSessions = 2,
            isRunning = true,
        )

        val result = session.tick()

        assertEquals(Pomodoro.FOCUS, result.session.phase)
        assertEquals(configuration.focusSeconds, result.session.remainingSeconds)
        assertEquals(2, result.session.completedFocusSessions)
        assertEquals(false, result.session.isRunning)
    }

    @Test
    fun `five minute threshold emits a warning without pausing the session`() {
        val session = PomodoroSession.start(testConfiguration()).copy(
            remainingSeconds = 5 * 60 + 1,
            isRunning = true,
        )

        val result = session.tick()

        val warning = assertIs<TimerEvent.TimeWarning>(result.event)
        assertEquals(Pomodoro.FOCUS, warning.phase)
        assertEquals(5 * 60, warning.secondsRemaining)
        assertEquals(true, result.session.isRunning)
    }

    @Test
    fun `pausing and continuing from shared timer state preserves the remaining time`() {
        val state = PomodoroTimerState()
        state.selectFocusType(testConfiguration())

        state.toggleRunning()
        state.tick()
        state.toggleRunning()

        assertEquals(false, state.session?.isRunning)
        assertEquals(25 * 60 - 1, state.session?.remainingSeconds)

        state.toggleRunning()

        assertEquals(true, state.session?.isRunning)
        assertEquals(25 * 60 - 1, state.session?.remainingSeconds)
    }

    @Test
    fun `fast-forward changes the interval without resetting the session`() {
        val state = PomodoroTimerState()
        state.selectFocusType(testConfiguration())

        state.changeSpeed(TimerSpeed.FAST)

        assertEquals(TimerSpeed.FAST, state.session?.speed)
        assertEquals(25 * 60, state.session?.remainingSeconds)
        assertEquals(false, state.session?.isRunning)
    }

    @Test
    fun `menu-bar state command keeps all fields in a stable order`() {
        val session = PomodoroSession.start(testConfiguration()).copy(
            remainingSeconds = 42,
            isRunning = true,
        )

        val command = MacMenuBarProtocol.updateCommand(session, "Preparar presentación")
        val fields = command.split("\t")

        assertEquals(listOf("state", "42", "1500", "RW5mb3F1ZQ==", "UHJlcGFyYXIgcHJlc2VudGFjacOzbg==", "1"), fields)
        assertEquals("idle", MacMenuBarProtocol.updateCommand(null, null))
        assertEquals(MacMenuBarAction.Hide, MacMenuBarProtocol.actionFrom("hide"))
        assertEquals(null, MacMenuBarProtocol.actionFrom("unsupported"))
    }

    @Test
    fun `completing a phase creates one completion notification and pauses the next phase`() {
        val state = PomodoroTimerState()
        state.selectFocusType(
            PomodoroConfiguration(
                name = "Pomodoro",
                focusSeconds = 1,
                shortBreakSeconds = 5 * 60,
                longBreakSeconds = 15 * 60,
                cyclesBeforeLongBreak = 4,
            )
        )
        state.toggleRunning()

        assertIs<TimerEvent.PhaseCompleted>(state.tick())
        assertEquals(Pomodoro.BREAK, state.session?.phase)
        assertEquals(false, state.session?.isRunning)
        assertEquals("Tiempo completado · Enfoque · Pomodoro", state.notification?.title)
        assertEquals(null, state.tick())
    }

    private fun testConfiguration(
        name: String = "Pomodoro",
        cycles: Int = 4,
    ) = PomodoroConfiguration(
        name = name,
        focusSeconds = 25 * 60,
        shortBreakSeconds = 5 * 60,
        longBreakSeconds = 15 * 60,
        cyclesBeforeLongBreak = cycles,
    )
}
