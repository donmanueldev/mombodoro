import Foundation

struct TimerState: Equatable {
    let remainingSeconds: Int
    let totalSeconds: Int
    let phase: String
    let taskTitle: String?
    let isRunning: Bool
}

enum MenuBarCommand: Equatable {
    case idle
    case state(TimerState)
    case notification(NativeNotification)
    case openNotificationSettings
}

struct NativeNotification: Equatable {
    let title: String
    let message: String
}

enum MenuBarProtocol {
    static func command(from line: String) -> MenuBarCommand? {
        let parts = line.split(separator: "\t", omittingEmptySubsequences: false)
        if parts.count == 1, parts[0] == "idle" { return .idle }
        if parts.count == 1, parts[0] == "openNotificationSettings" { return .openNotificationSettings }

        if
            parts.count == 3,
            parts[0] == "notification",
            let title = decode(parts[1]),
            let message = decode(parts[2])
        {
            return .notification(
                NativeNotification(
                    title: title,
                    message: message
                )
            )
        }

        guard
            parts.count == 6,
            parts[0] == "state",
            let remainingSeconds = Int(parts[1]),
            let totalSeconds = Int(parts[2]),
            let phase = decode(parts[3]),
            let taskTitle = decodeOptional(parts[4]),
            parts[5] == "0" || parts[5] == "1"
        else { return nil }

        return .state(
            TimerState(
                remainingSeconds: remainingSeconds,
                totalSeconds: totalSeconds,
                phase: phase,
                taskTitle: taskTitle,
                isRunning: parts[5] == "1"
            )
        )
    }

    static func tooltip(for state: TimerState) -> String {
        let time = String(format: "%02d:%02d", state.remainingSeconds / 60, state.remainingSeconds % 60)
        return "Mombodoro · \(time) · \(state.phase)\(state.isRunning ? "" : " · Pausado")"
    }

    private static func decode(_ value: Substring) -> String? {
        guard let data = Data(base64Encoded: String(value)) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func decodeOptional(_ value: Substring) -> String? {
        value == "-" ? nil : decode(value)
    }
}
