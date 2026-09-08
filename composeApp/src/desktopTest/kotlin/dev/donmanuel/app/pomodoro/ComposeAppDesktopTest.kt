package dev.donmanuel.app.pomodoro

import dev.donmanuel.app.pomodoro.data.PomodoroConfiguration
import dev.donmanuel.app.pomodoro.data.PomodoroSession
import dev.donmanuel.app.pomodoro.data.Pomodoro
import dev.donmanuel.app.pomodoro.data.TimerEvent
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
