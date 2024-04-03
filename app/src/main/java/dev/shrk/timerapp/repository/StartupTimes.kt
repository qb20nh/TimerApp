package dev.shrk.timerapp.repository

import java.time.Instant

class StartupTimes {
    companion object {
        lateinit var UNLOCK: Instant
        lateinit var ON: Instant
        lateinit var RESUME: Instant
    }
}