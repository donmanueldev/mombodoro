import Cocoa

final class MenuBarHost: NSObject, NSApplicationDelegate {
    private let statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
    private let menu = NSMenu()
    private let toggleItem = NSMenuItem(title: "Continuar temporizador", action: #selector(toggleTimer), keyEquivalent: "")
    private var inputBuffer = ""

    func applicationDidFinishLaunching(_ notification: Notification) {
        statusItem.button?.title = "Mombodoro"
        toggleItem.target = self
        toggleItem.isEnabled = false
        menu.addItem(toggleItem)
        menu.addItem(.separator())
        menu.addItem(menuItem("Mostrar Mombodoro", action: #selector(showWindow)))
        menu.addItem(menuItem("Salir", action: #selector(exitApplication)))
        statusItem.menu = menu

        FileHandle.standardInput.readabilityHandler = { [weak self] handle in
            let data = handle.availableData
            guard !data.isEmpty, let chunk = String(data: data, encoding: .utf8) else { return }
            DispatchQueue.main.async { self?.consume(chunk) }
        }
    }

    @objc private func toggleTimer() { emit("toggle") }
    @objc private func showWindow() { emit("show") }
    @objc private func exitApplication() { emit("exit") }

    private func menuItem(_ title: String, action: Selector) -> NSMenuItem {
        let item = NSMenuItem(title: title, action: action, keyEquivalent: "")
        item.target = self
        return item
    }

    private func consume(_ chunk: String) {
        inputBuffer += chunk
        let lines = inputBuffer.split(separator: "\n", omittingEmptySubsequences: false)
        inputBuffer = lines.last.map(String.init) ?? ""

        for line in lines.dropLast() {
            let parts = line.split(separator: "\t", omittingEmptySubsequences: false)
            guard parts.count == 3, parts[0] == "state", let title = decode(parts[1]) else { continue }
            statusItem.button?.title = title
            let isRunning = parts[2] == "1"
            toggleItem.title = isRunning ? "Parar temporizador" : "Continuar temporizador"
            toggleItem.isEnabled = title != "Mombodoro"
        }
    }

    private func decode(_ value: Substring) -> String? {
        guard let data = Data(base64Encoded: String(value)) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func emit(_ value: String) {
        FileHandle.standardOutput.write(Data("\(value)\n".utf8))
    }
}

let application = NSApplication.shared
application.setActivationPolicy(.accessory)
let delegate = MenuBarHost()
application.delegate = delegate
application.run()
