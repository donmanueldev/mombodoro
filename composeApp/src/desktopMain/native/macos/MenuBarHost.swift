import Cocoa
import UserNotifications

final class MenuBarHost: NSObject, NSApplicationDelegate, UNUserNotificationCenterDelegate {
    private enum NotificationAuthorization {
        case pending
        case authorized
        case denied
    }

    private let statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
    private let menu = NSMenu()
    private var notificationAuthorization: NotificationAuthorization = .pending
    private var pendingNotifications: [NativeNotification] = []
    private lazy var statusImage: NSImage? = {
        let iconPath = ProcessInfo.processInfo.environment["MOMBODORO_STATUS_ICON"]
        let image =
            iconPath.flatMap { NSImage(contentsOfFile: $0) }
            ?? NSImage(systemSymbolName: "timer", accessibilityDescription: "Mombodoro")
        image?.isTemplate = true
        image?.size = NSSize(width: 18, height: 18)
        return image
    }()

    func applicationDidFinishLaunching(_ notification: Notification) {
        let notifications = UNUserNotificationCenter.current()
        notifications.delegate = self
        notifications.requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] granted, _ in
            DispatchQueue.main.async {
                guard let self else { return }
                guard granted else {
                    self.notificationAuthorization = .denied
                    self.emit("notificationPermissionDenied")
                    self.pendingNotifications.removeAll()
                    return
                }

                self.notificationAuthorization = .authorized
                self.emit("notificationPermissionGranted")
                let queuedNotifications = self.pendingNotifications
                self.pendingNotifications.removeAll()
                queuedNotifications.forEach(self.deliver)
            }
        }
        configureStatusButton()
        configureMenu()
        showTimer("--:--")

        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            while let line = readLine() {
                DispatchQueue.main.async { self?.consume(line) }
            }
        }
    }

    private func configureStatusButton() {
        guard let button = statusItem.button else { return }
        button.target = self
        button.action = #selector(handleStatusClick)
        button.sendAction(on: [.leftMouseUp, .rightMouseUp])
        button.imagePosition = .imageLeft
    }

    private func configureMenu() {
        menu.addItem(menuItem("Mostrar Mombodoro", action: #selector(showWindow)))
        menu.addItem(menuItem("Ocultar Mombodoro", action: #selector(hideWindow)))
        menu.addItem(.separator())
        menu.addItem(menuItem("Salir", action: #selector(exitApplication)))
    }

    @objc private func handleStatusClick() {
        if NSApp.currentEvent?.type == .rightMouseUp {
            guard let button = statusItem.button else { return }
            menu.popUp(positioning: nil, at: NSPoint(x: 0, y: button.bounds.height), in: button)
        } else {
            showWindow()
        }
    }

    @objc private func showWindow() { emit("show") }
    @objc private func hideWindow() { emit("hide") }
    @objc private func exitApplication() { emit("exit") }

    private func menuItem(_ title: String, action: Selector) -> NSMenuItem {
        let item = NSMenuItem(title: title, action: action, keyEquivalent: "")
        item.target = self
        return item
    }

    private func consume(_ line: String) {
        switch MenuBarProtocol.command(from: line) {
        case .timer(let title):
            showTimer(title)
        case .notification(let notification):
            deliver(notification)
        case .openNotificationSettings:
            openNotificationSettings()
        case nil:
            return
        }
    }

    private func deliver(_ notification: NativeNotification) {
        switch notificationAuthorization {
        case .pending:
            pendingNotifications.append(notification)
            return
        case .denied:
            emit("notificationPermissionDenied")
            return
        case .authorized:
            break
        }

        let content = UNMutableNotificationContent()
        content.title = notification.title
        content.body = notification.message
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request) { [weak self] error in
            guard error != nil else { return }
            DispatchQueue.main.async { self?.emit("notificationDeliveryFailed") }
        }
    }

    private func openNotificationSettings() {
        guard let url = URL(string: "x-apple.systempreferences:com.apple.Notifications-Settings.extension") else { return }
        NSWorkspace.shared.open(url)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        emit("notificationOpened")
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    private func showTimer(_ title: String) {
        renderStatus(
            title: title,
            tooltip: "Mombodoro · \(title)"
        )
    }

    private func renderStatus(title: String, tooltip: String) {
        guard let button = statusItem.button else { return }
        button.image = statusImage
        button.imagePosition = .imageLeft
        button.title = title
        button.toolTip = tooltip
    }

    private func emit(_ value: String) {
        FileHandle.standardOutput.write(Data("\(value)\n".utf8))
    }
}

@main
struct MenuBarApplication {
    static func main() {
        let application = NSApplication.shared
        application.setActivationPolicy(.accessory)
        let delegate = MenuBarHost()
        application.delegate = delegate
        application.run()
    }
}
