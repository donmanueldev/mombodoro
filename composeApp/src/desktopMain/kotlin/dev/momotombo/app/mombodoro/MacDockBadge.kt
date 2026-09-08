package dev.momotombo.app.mombodoro

/** Updates Mombodoro's own Dock tile without giving the menu-bar helper a second Dock icon. */
internal object MacDockBadge {
    fun update(hasPendingAttention: Boolean) {
        if (System.getProperty("os.name") != "Mac OS X") return

        runCatching {
            val application = Class.forName("com.apple.eawt.Application")
            val currentApplication = application.getMethod("getApplication").invoke(null)
            application.getMethod("setDockIconBadge", String::class.java)
                .invoke(currentApplication, if (hasPendingAttention) "1" else null)
        }
    }
}
