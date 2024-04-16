package io.github.konstantinberkow.mockexchange

import android.app.Application
import io.github.konstantinberkow.mockexchange.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MockExchangeApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            if (BuildConfig.DEBUG) {
                androidLogger(level = Level.DEBUG)
            }
            androidContext(this@MockExchangeApp)
            modules(appModule)
        }
    }
}
