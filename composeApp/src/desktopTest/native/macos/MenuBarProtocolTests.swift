import Foundation

@main
struct MenuBarProtocolTests {
    static func main() {
        let command = MenuBarProtocol.command(from: "timer\tMDA6NDI=")
        guard case let .timer(title) = command else { fatalError("Expected a timer command") }
        expect(title == "00:42", "timer title")
        expect(MenuBarProtocol.command(from: "timer\tnot-base64") == nil, "invalid timer title")
        expect(MenuBarProtocol.command(from: "openNotificationSettings") == .openNotificationSettings, "open notification settings command")

        let notification = MenuBarProtocol.command(from: "notification\tVGllbXBvIGNvbXBsZXRhZG8=\tRGVzY2Fuc2E=")
        guard case let .notification(alert) = notification else { fatalError("Expected a notification command") }
        expect(alert.title == "Tiempo completado", "notification title")
        expect(alert.message == "Descansa", "notification message")

        let testNotification = MenuBarProtocol.command(from: "testNotification\tUHJ1ZWJhIGRlIE1vbWJvZG9ybw==\tRnVuY2lvbmE=")
        guard case let .testNotification(testAlert) = testNotification else { fatalError("Expected a test notification command") }
        expect(testAlert.title == "Prueba de Mombodoro", "test notification title")
        expect(testAlert.message == "Funciona", "test notification message")
    }

    private static func expect(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() { fatalError(message) }
    }
}
