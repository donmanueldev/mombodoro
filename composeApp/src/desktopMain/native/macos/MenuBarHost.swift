import Cocoa

private final class TimerRingView: NSView {
    var progress: CGFloat = 1 { didSet { needsDisplay = true } }

    override func draw(_ dirtyRect: NSRect) {
        super.draw(dirtyRect)
        let lineWidth: CGFloat = 10
        let ringRect = bounds.insetBy(dx: lineWidth / 2, dy: lineWidth / 2)
        let center = NSPoint(x: ringRect.midX, y: ringRect.midY)
        let radius = min(ringRect.width, ringRect.height) / 2

        let track = NSBezierPath(ovalIn: ringRect)
        track.lineWidth = lineWidth
        NSColor.separatorColor.withAlphaComponent(0.45).setStroke()
        track.stroke()

        guard progress > 0 else { return }
        let arc = NSBezierPath()
        arc.appendArc(
            withCenter: center,
            radius: radius,
            startAngle: 90,
            endAngle: 90 - (360 * min(max(progress, 0), 1)),
            clockwise: true
        )
        arc.lineWidth = lineWidth
        arc.lineCapStyle = .round
        NSColor(calibratedRed: 0.78, green: 0.33, blue: 0.28, alpha: 1).setStroke()
        arc.stroke()
    }
}

private final class TimerPopoverController: NSViewController {
    var onToggle: (() -> Void)?

    private let ringView = TimerRingView()
    private let timeLabel = NSTextField(labelWithString: "—")
    private let phaseLabel = NSTextField(labelWithString: "Sin temporizador")
    private let taskLabel = NSTextField(labelWithString: "Elige una tarea en Mombodoro")
    private let toggleButton = NSButton(title: "Empezar", target: nil, action: nil)

    override func loadView() {
        view = NSView(frame: NSRect(x: 0, y: 0, width: 300, height: 350))
        preferredContentSize = NSSize(width: 300, height: 350)
        view.wantsLayer = true
        view.layer?.backgroundColor = NSColor.windowBackgroundColor.cgColor

        let timerContainer = NSView()
        [timerContainer, ringView, timeLabel, phaseLabel, taskLabel, toggleButton].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false
        }
        view.addSubview(timerContainer)
        timerContainer.addSubview(ringView)
        timerContainer.addSubview(timeLabel)
        timerContainer.addSubview(phaseLabel)
        view.addSubview(toggleButton)

        timeLabel.font = NSFont.systemFont(ofSize: 52, weight: .bold)
        timeLabel.textColor = .labelColor
        timeLabel.alignment = .center

        phaseLabel.font = NSFont.systemFont(ofSize: 13, weight: .semibold)
        phaseLabel.textColor = .secondaryLabelColor
        phaseLabel.alignment = .center
        phaseLabel.wantsLayer = true
        phaseLabel.layer?.backgroundColor = NSColor.controlBackgroundColor.cgColor
        phaseLabel.layer?.cornerRadius = 14
        phaseLabel.layer?.masksToBounds = true

        taskLabel.font = NSFont.systemFont(ofSize: 12, weight: .medium)
        taskLabel.textColor = .secondaryLabelColor
        taskLabel.alignment = .center
        taskLabel.lineBreakMode = .byTruncatingTail
        taskLabel.maximumNumberOfLines = 1

        toggleButton.target = self
        toggleButton.action = #selector(toggleTimer)
        toggleButton.font = NSFont.systemFont(ofSize: 14, weight: .bold)
        toggleButton.contentTintColor = .white
        toggleButton.isBordered = false
        toggleButton.wantsLayer = true
        toggleButton.layer?.backgroundColor = NSColor(calibratedRed: 0.78, green: 0.33, blue: 0.28, alpha: 1).cgColor
        toggleButton.layer?.cornerRadius = 14

        NSLayoutConstraint.activate([
            timerContainer.topAnchor.constraint(equalTo: view.topAnchor, constant: 22),
            timerContainer.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            timerContainer.widthAnchor.constraint(equalToConstant: 210),
            timerContainer.heightAnchor.constraint(equalToConstant: 210),
            ringView.leadingAnchor.constraint(equalTo: timerContainer.leadingAnchor),
            ringView.trailingAnchor.constraint(equalTo: timerContainer.trailingAnchor),
            ringView.topAnchor.constraint(equalTo: timerContainer.topAnchor),
            ringView.bottomAnchor.constraint(equalTo: timerContainer.bottomAnchor),
            timeLabel.centerXAnchor.constraint(equalTo: timerContainer.centerXAnchor),
            timeLabel.centerYAnchor.constraint(equalTo: timerContainer.centerYAnchor, constant: -14),
            phaseLabel.centerXAnchor.constraint(equalTo: timerContainer.centerXAnchor),
            phaseLabel.topAnchor.constraint(equalTo: timeLabel.bottomAnchor, constant: 8),
            phaseLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 130),
            phaseLabel.heightAnchor.constraint(equalToConstant: 28),
            taskLabel.topAnchor.constraint(equalTo: timerContainer.bottomAnchor, constant: 14),
            taskLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 28),
            taskLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -28),
            taskLabel.heightAnchor.constraint(equalToConstant: 18),
            toggleButton.topAnchor.constraint(equalTo: taskLabel.bottomAnchor, constant: 20),
            toggleButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            toggleButton.widthAnchor.constraint(equalToConstant: 188),
            toggleButton.heightAnchor.constraint(equalToConstant: 48),
            toggleButton.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -18),
        ])
    }

    func update(with state: TimerState?) {
        guard let state else {
            timeLabel.stringValue = "—"
            phaseLabel.stringValue = "Sin temporizador"
            taskLabel.stringValue = "Elige una tarea en Mombodoro"
            ringView.progress = 0
            toggleButton.title = "Elige un enfoque"
            toggleButton.isEnabled = false
            toggleButton.alphaValue = 0.5
            return
        }

        timeLabel.stringValue = String(format: "%02d:%02d", state.remainingSeconds / 60, state.remainingSeconds % 60)
        phaseLabel.stringValue = state.phase
        taskLabel.stringValue = state.taskTitle ?? "Elige una tarea en Mombodoro"
        ringView.progress = CGFloat(state.remainingSeconds) / CGFloat(max(state.totalSeconds, 1))
        toggleButton.title = state.isRunning ? "Pausar" : "Empezar"
        toggleButton.isEnabled = true
        toggleButton.alphaValue = 1
    }

    @objc private func toggleTimer() { onToggle?() }
}

final class MenuBarHost: NSObject, NSApplicationDelegate {
    private let statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
    private let menu = NSMenu()
    private let popoverController = TimerPopoverController()
    private let popover = NSPopover()
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
        configureStatusButton()
        configurePopover()
        configureMenu()
        showIdleStatus()

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

    private func configurePopover() {
        popover.contentViewController = popoverController
        popover.contentSize = NSSize(width: 300, height: 350)
        popover.behavior = .transient
        popover.animates = true
        popoverController.onToggle = { [weak self] in self?.emit("toggle") }
    }

    private func configureMenu() {
        menu.addItem(menuItem("Mostrar Mombodoro", action: #selector(showWindow)))
        menu.addItem(menuItem("Ocultar Mombodoro", action: #selector(hideWindow)))
        menu.addItem(.separator())
        menu.addItem(menuItem("Salir", action: #selector(exitApplication)))
    }

    @objc private func handleStatusClick() {
        guard let button = statusItem.button else { return }
        if NSApp.currentEvent?.type == .rightMouseUp {
            menu.popUp(positioning: nil, at: NSPoint(x: 0, y: button.bounds.height), in: button)
        } else if popover.isShown {
            popover.performClose(nil)
        } else {
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
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
        case .idle:
            showIdleStatus()
        case .state(let state):
            showActiveStatus(state)
        case nil:
            return
        }
    }

    private func showIdleStatus() {
        statusItem.button?.image = statusImage
        statusItem.button?.title = "--:--"
        statusItem.button?.toolTip = "Mombodoro · Sin temporizador"
        popoverController.update(with: nil)
    }

    private func showActiveStatus(_ state: TimerState) {
        statusItem.button?.image = statusImage
        statusItem.button?.title = String(format: "%02d:%02d", state.remainingSeconds / 60, state.remainingSeconds % 60)
        statusItem.button?.toolTip = MenuBarProtocol.tooltip(for: state)
        popoverController.update(with: state)
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
