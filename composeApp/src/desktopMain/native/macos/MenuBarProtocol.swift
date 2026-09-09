import Foundation

enum MenuBarCommand: Equatable {
    case timer(String)
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
        if parts.count == 1, parts[0] == "openNotificationSettings" { return .openNotificationSettings }

        if parts.count == 2, parts[0] == "timer", let title = decode(parts[1]) {
            return .timer(title)
        }

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

        return nil
    }

    private static func decode(_ value: Substring) -> String? {
        guard let data = Data(base64Encoded: String(value)) else { return nil }
        return String(data: data, encoding: .utf8)
    }
}
