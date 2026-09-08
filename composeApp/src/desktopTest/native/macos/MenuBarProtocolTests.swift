import Foundation

@main
struct MenuBarProtocolTests {
    static func main() {
        let command = MenuBarProtocol.command(from: "state\t42\t1500\tRW5mb3F1ZQ==\tUHJlcGFyYXI=\t0")
        guard case let .state(state) = command else { fatalError("Expected a state command") }
        expect(state.remainingSeconds == 42, "remaining seconds")
        expect(state.totalSeconds == 1500, "total seconds")
        expect(state.phase == "Enfoque", "phase")
        expect(state.taskTitle == "Preparar", "task")
        expect(!state.isRunning, "paused state")
        expect(MenuBarProtocol.tooltip(for: state) == "Mombodoro · 00:42 · Enfoque · Pausado", "tooltip")
        expect(MenuBarProtocol.command(from: "state\t42\t1500\tRW5mb3F1ZQ==\t-\t2") == nil, "invalid running state")
        expect(MenuBarProtocol.command(from: "idle") == .idle, "idle command")
        expect(MenuBarProtocol.command(from: "openNotificationSettings") == .openNotificationSettings, "open notification settings command")

        let notification = MenuBarProtocol.command(from: "notification\tVGllbXBvIGNvbXBsZXRhZG8=\tRGVzY2Fuc2E=")
        guard case let .notification(alert) = notification else { fatalError("Expected a notification command") }
        expect(alert.title == "Tiempo completado", "notification title")
        expect(alert.message == "Descansa", "notification message")
    }

    private static func expect(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fatalError(message) }
    }
}
