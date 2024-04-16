package io.github.konstantinberkow.mockexchange

import android.app.Application
import io.github.konstantinberkow.mockexchange.di.appModule
import io.github.konstantinberkow.mockexchange.koinx.KoinTimberLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.EmptyLogger
import timber.log.Timber

class MockExchangeApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val koinLogger = if (BuildConfig.DEBUG) {
            KoinTimberLogger()
        } else {
            EmptyLogger()
        }

        startKoin {
            logger(koinLogger)
            androidContext(this@MockExchangeApp)
            modules(appModule)
        }
    }
}
