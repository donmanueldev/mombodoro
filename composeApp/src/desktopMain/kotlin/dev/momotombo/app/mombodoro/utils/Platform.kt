package dev.momotombo.app.mombodoro.utils

interface Platform {
    val isDesktop: Boolean
}

fun platform(): Platform = object : Platform {
    override val isDesktop: Boolean = true
}
