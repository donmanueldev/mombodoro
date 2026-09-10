package dev.momotombo.app.mombodoro.presentation

import dev.momotombo.app.mombodoro.MacNotificationStatus
import dev.momotombo.app.mombodoro.MacNotificationTestStatus

data class NotificationSettingsState(
    val permission: MacNotificationStatus?,
    val test: MacNotificationTestStatus,
) {
    val permissionLabel: String
        get() = when (permission) {
            MacNotificationStatus.Enabled -> "Avisos activados"
            MacNotificationStatus.Disabled -> "Avisos desactivados"
            MacNotificationStatus.Checking -> "Comprobando permisos…"
            null -> "Integración no disponible"
        }

    val testResultLabel: String?
        get() = when (test) {
            MacNotificationTestStatus.Idle -> null
            MacNotificationTestStatus.Testing -> "Enviando prueba…"
            MacNotificationTestStatus.Delivered -> "Prueba entregada a macOS."
            MacNotificationTestStatus.Denied -> "Prueba denegada por macOS."
            MacNotificationTestStatus.Failed -> "La prueba falló."
        }

    val canTest: Boolean
        get() = permission != null &&
            permission != MacNotificationStatus.Checking &&
            test != MacNotificationTestStatus.Testing

    val shouldOfferSystemSettings: Boolean
        get() = permission == MacNotificationStatus.Disabled
}

enum class NotificationSettingsAction {
    Test,
    OpenSystemSettings,
}
