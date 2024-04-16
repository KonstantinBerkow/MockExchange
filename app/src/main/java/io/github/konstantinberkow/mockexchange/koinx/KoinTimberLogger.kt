package io.github.konstantinberkow.mockexchange.koinx

import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import timber.log.Timber

class KoinTimberLogger(level: Level = Level.INFO) : Logger(level = level) {

    override fun display(level: Level, msg: MESSAGE) {
        when (level) {
            Level.DEBUG -> Timber.d(msg)
            Level.INFO -> Timber.i(msg)
            Level.WARNING -> Timber.w(msg)
            Level.ERROR -> Timber.e(msg)
            Level.NONE -> Timber.wtf(msg)
        }
    }
}