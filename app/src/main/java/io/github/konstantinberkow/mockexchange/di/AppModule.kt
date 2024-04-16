package io.github.konstantinberkow.mockexchange.di

import android.util.Log
import com.google.gson.GsonBuilder
import io.github.konstantinberkow.mockexchange.BuildConfig
import io.github.konstantinberkow.mockexchange.entity.source.ExchangeRatesSource
import io.github.konstantinberkow.mockexchange.remote.ExchangeRatesApi
import io.github.konstantinberkow.mockexchange.remote.NetworkExchangeRatesSource
import io.github.konstantinberkow.mockexchange.remote.dto.RemoteExchangeData
import io.github.konstantinberkow.mockexchange.remote.serialization.RemoteExchangeDataGsonTypeAdapter
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.time.DurationUnit
import kotlin.time.toDuration

val appModule = module {

    single {
        GsonBuilder()
            .registerTypeAdapter(
                RemoteExchangeData::class.java,
                RemoteExchangeDataGsonTypeAdapter()
            )
            .create()
    }

    single {
        OkHttpClient.Builder()
            .also {
                if (BuildConfig.DEBUG) {
                    val loggingInterceptor = HttpLoggingInterceptor { message ->
                        Log.v("Retrofit", message)
                    }
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
                    it.addInterceptor(loggingInterceptor)
                }
            }
            .build()
    }

    single {
        val baseUrl = BuildConfig.API_URL
        val retrofitInstance = Retrofit.Builder()
            .client(get())
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(get()))
            .validateEagerly(true)
            .build()

        retrofitInstance.create(ExchangeRatesApi::class.java)
    }

    single<ExchangeRatesSource> {
        NetworkExchangeRatesSource(
            api = get(),
            dispatcher = Dispatchers.IO,
            refreshDelay = 5.toDuration(unit = DurationUnit.SECONDS)
        )
    }
}