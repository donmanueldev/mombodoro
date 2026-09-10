package dev.momotombo.app.mombodoro

import dev.momotombo.app.mombodoro.data.Pomodoro
import dev.momotombo.app.mombodoro.data.PomodoroConfiguration
import dev.momotombo.app.mombodoro.data.PomodoroSession
import dev.momotombo.app.mombodoro.data.TimerEvent
import dev.momotombo.app.mombodoro.data.TimerSpeed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `completing a long break returns to focus`() {
        val configuration = testConfiguration(cycles = 4)
        val session = PomodoroSession.start(configuration).copy(
            phase = Pomodoro.LONG_BREAK,
            remainingSeconds = 1,
            isRunning = true,
        )

        val result = session.tick()

        assertEquals(Pomodoro.FOCUS, result.session.phase)
        assertEquals(configuration.focusSeconds, result.session.remainingSeconds)
        assertEquals(false, result.session.isRunning)
        assertIs<TimerEvent.PhaseCompleted>(result.event)
    }

    @Test
    fun `configuration rejects blank names and non-positive values`() {
        assertFailsWith<IllegalArgumentException> {
            testConfiguration(name = " ")
        }
        assertFailsWith<IllegalArgumentException> {
            PomodoroConfiguration(
                name = "Inválido",
                focusSeconds = 0,
                shortBreakSeconds = 1,
                longBreakSeconds = 1,
                cyclesBeforeLongBreak = 1,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            testConfiguration(cycles = 0)
        }
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
    fun `three minute threshold emits its warning`() {
        val session = PomodoroSession.start(testConfiguration()).copy(
            remainingSeconds = 3 * 60 + 1,
            isRunning = true,
        )

        val result = session.tick()

        val warning = assertIs<TimerEvent.TimeWarning>(result.event)
        assertEquals(3 * 60, warning.secondsRemaining)
        assertEquals(3 * 60, result.session.remainingSeconds)
    }

    @Test
    fun `stopped session ignores elapsed time`() {
        val session = PomodoroSession.start(testConfiguration())

        val result = session.advanceBy(30)

        assertEquals(session, result.session)
        assertEquals(null, result.event)
    }

    @Test
    fun `elapsed time cannot be negative`() {
        val session = PomodoroSession.start(testConfiguration()).copy(isRunning = true)

        assertFailsWith<IllegalArgumentException> {
            session.advanceBy(-1)
        }
    }

    @Test
    fun `pausing and continuing from shared timer state preserves the remaining time`() {
        val clock = FakeMonotonicClock()
        val state = PomodoroTimerState(clock)
        state.selectFocusType(testConfiguration())

        state.toggleRunning()
        clock.advanceSeconds(1)
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
    fun `menu-bar timer command contains the rendered session time`() {
        val session = PomodoroSession.start(testConfiguration()).copy(
            remainingSeconds = 42,
            isRunning = true,
        )

        val command = MacMenuBarProtocol.timerCommand(session)
        val fields = command.split("\t")

        assertEquals(listOf("timer", "MDA6NDI="), fields)
        assertEquals(listOf("timer", "LS06LS0="), MacMenuBarProtocol.timerCommand(null).split("\t"))
        assertEquals(MacMenuBarAction.Hide, MacMenuBarProtocol.actionFrom("hide"))
        assertEquals(null, MacMenuBarProtocol.actionFrom("unsupported"))
    }

    @Test
    fun `native notification command preserves its title and message`() {
        val command = MacMenuBarProtocol.notificationCommand(
            TimerNotification(
                phase = Pomodoro.FOCUS,
                title = "Alerta de tiempo · Enfoque · Pomodoro",
                message = "Quedan 5 minutos para terminar.",
                requiresAttention = false,
            )
        )

        assertEquals(
            "notification\tQWxlcnRhIGRlIHRpZW1wbyDCtyBFbmZvcXVlIMK3IFBvbW9kb3Jv\tUXVlZGFuIDUgbWludXRvcyBwYXJhIHRlcm1pbmFyLg==",
            command,
        )
        assertEquals(MacMenuBarAction.NotificationOpened, MacMenuBarProtocol.actionFrom("notificationOpened"))
        assertEquals(MacMenuBarAction.NotificationPermissionGranted, MacMenuBarProtocol.actionFrom("notificationPermissionGranted"))
        assertEquals(MacMenuBarAction.NotificationPermissionDenied, MacMenuBarProtocol.actionFrom("notificationPermissionDenied"))
        assertEquals(MacMenuBarAction.NotificationDeliveryFailed, MacMenuBarProtocol.actionFrom("notificationDeliveryFailed"))
        assertEquals(MacMenuBarAction.NotificationTestDelivered, MacMenuBarProtocol.actionFrom("notificationTestDelivered"))
        assertEquals(MacMenuBarAction.NotificationTestDenied, MacMenuBarProtocol.actionFrom("notificationTestDenied"))
        assertEquals(MacMenuBarAction.NotificationTestFailed, MacMenuBarProtocol.actionFrom("notificationTestFailed"))
        assertEquals(MacMenuBarAction.HostFailed, MacMenuBarProtocol.actionFrom("hostFailed"))
        assertEquals(
            "testNotification\tUHJ1ZWJhIGRlIE1vbWJvZG9ybw==\tTGFzIG5vdGlmaWNhY2lvbmVzIG5hdGl2YXMgZnVuY2lvbmFuIGNvcnJlY3RhbWVudGUu",
            MacMenuBarProtocol.testNotificationCommand(),
        )
    }

    @Test
    fun `completing a phase creates one completion notification and pauses the next phase`() {
        val clock = FakeMonotonicClock()
        val state = PomodoroTimerState(clock)
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
        clock.advanceSeconds(1)

        assertIs<TimerEvent.PhaseCompleted>(state.tick())
        assertEquals(Pomodoro.BREAK, state.session?.phase)
        assertEquals(false, state.session?.isRunning)
        assertEquals("Tiempo completado · Enfoque · Pomodoro", state.notification?.title)
        assertEquals(true, state.notification?.requiresAttention)
        assertEquals(null, state.tick())
    }

    @Test
    fun `time warnings create native alerts without requiring dock attention`() {
        val clock = FakeMonotonicClock()
        val state = PomodoroTimerState(clock)
        state.selectFocusType(
            PomodoroConfiguration(
                name = "Pomodoro",
                focusSeconds = 5 * 60 + 1,
                shortBreakSeconds = 5 * 60,
                longBreakSeconds = 15 * 60,
                cyclesBeforeLongBreak = 4,
            )
        )
        state.toggleRunning()
        clock.advanceSeconds(1)

        assertIs<TimerEvent.TimeWarning>(state.tick())
        assertEquals(false, state.notification?.requiresAttention)
    }

    @Test
    fun `dismissing a completion alert clears its pending attention`() {
        val clock = FakeMonotonicClock()
        val state = PomodoroTimerState(clock)
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
        clock.advanceSeconds(1)
        state.tick()

        state.dismissNotification()

        assertEquals(null, state.notification)
    }

    @Test
    fun `timer catches up after the process is suspended`() {
        val clock = FakeMonotonicClock()
        val state = PomodoroTimerState(clock)
        state.selectFocusType(testConfiguration())
        state.toggleRunning()

        clock.advanceSeconds(37)
        state.tick()

        assertEquals(25 * 60 - 37, state.session?.remainingSeconds)
    }

    @Test
    fun `catching up past the deadline completes the phase once`() {
        val clock = FakeMonotonicClock()
        val state = PomodoroTimerState(clock)
        state.selectFocusType(
            PomodoroConfiguration(
                name = "Breve",
                focusSeconds = 5,
                shortBreakSeconds = 60,
                longBreakSeconds = 120,
                cyclesBeforeLongBreak = 4,
            )
        )
        state.toggleRunning()

        clock.advanceSeconds(30)

        assertIs<TimerEvent.PhaseCompleted>(state.tick())
        assertEquals(Pomodoro.BREAK, state.session?.phase)
        assertEquals(false, state.session?.isRunning)
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

    private class FakeMonotonicClock : MonotonicClock {
        private var nowNanos = 0L

        override fun nowNanos(): Long = nowNanos

        fun advanceSeconds(seconds: Long) {
            nowNanos += seconds * 1_000_000_000L
        }
    }
}
