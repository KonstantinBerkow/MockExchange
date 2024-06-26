package io.github.konstantinberkow.mockexchange.di

import com.google.gson.GsonBuilder
import io.github.konstantinberkow.mockexchange.BuildConfig
import io.github.konstantinberkow.mockexchange.data.CurrenciesRepository
import io.github.konstantinberkow.mockexchange.data.ExchangeHistoryRepository
import io.github.konstantinberkow.mockexchange.data.FavoriteCurrenciesRepository
import io.github.konstantinberkow.mockexchange.data.FixedFavoriteCurrenciesRepository
import io.github.konstantinberkow.mockexchange.data.InMemoryBalancesRepository
import io.github.konstantinberkow.mockexchange.data.InMemoryExchangeHistoryRepository
import io.github.konstantinberkow.mockexchange.data.UserBalancesRepository
import io.github.konstantinberkow.mockexchange.entity.Currency
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.DischargeFeeUseCase
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.ExchangeCurrenciesUseCase
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.ExchangeError
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.NoFeeDischargeUseCase
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.StaticFeeUseCase
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.SwitchingDischargeFeeUseCase
import io.github.konstantinberkow.mockexchange.remote.ExchangeRatesApi
import io.github.konstantinberkow.mockexchange.remote.data.NetworkCurrenciesRepository
import io.github.konstantinberkow.mockexchange.remote.dto.RemoteExchangeData
import io.github.konstantinberkow.mockexchange.remote.rules.LatestDataExchangeRule
import io.github.konstantinberkow.mockexchange.remote.rules.SingleCurrencyBasedExchangeRule
import io.github.konstantinberkow.mockexchange.remote.serialization.RemoteExchangeDataGsonTypeAdapter
import io.github.konstantinberkow.mockexchange.remote.source.ExchangeRatesSource
import io.github.konstantinberkow.mockexchange.remote.source.NetworkExchangeRatesSource
import io.github.konstantinberkow.mockexchange.remote.source.StartWithExchangeRatesSource
import io.github.konstantinberkow.mockexchange.ui.overview.OverviewViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(DelicateCoroutinesApi::class)
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
                        Timber.tag("OkHttp").v(message)
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
        val coroutineScope = GlobalScope
        StartWithExchangeRatesSource(
            firstValue = RemoteExchangeData(
                base = Currency.EUR,
                exchangeRates = mapOf(
                    Currency("UAH") to 31.018778,
                    Currency("USD") to 1.129031,
                    Currency("TRY") to 15.612274,
                )
            ),
            delegate = NetworkExchangeRatesSource(
                api = get(),
                dispatcher = Dispatchers.IO,
                refreshDelay = 5.toDuration(unit = DurationUnit.SECONDS),
                shareScope = coroutineScope
            ),
            shareScope = coroutineScope
        )
    }

    single<CurrenciesRepository> {
        NetworkCurrenciesRepository(
            exchangeRatesSource = get()
        )
    }

    single<ExchangeCurrenciesUseCase<UInt, ExchangeError>> {
        LatestDataExchangeRule(
            exchangeRatesSource = get(),
            ruleFromData = { base, knownRates ->
                SingleCurrencyBasedExchangeRule(base = base, knownRates = knownRates)
            }
        )
    }

    single<ExchangeHistoryRepository> {
        InMemoryExchangeHistoryRepository(
            initialExchangesCount = 0u
        )
    }

    single<DischargeFeeUseCase<UInt>> {
        val historyRepo: ExchangeHistoryRepository = get()
        SwitchingDischargeFeeUseCase(
            signal = historyRepo.exchangesCount(),
            delegate = { exchangesCount ->
                if (exchangesCount < 5u) {
                    NoFeeDischargeUseCase()
                } else {
                    StaticFeeUseCase(percent = 0.007F)
                }
            },
            scope = GlobalScope
        )
    }

    single<UserBalancesRepository> {
        InMemoryBalancesRepository(
            initialBalances = mapOf(Currency.EUR to 100000u),
            exchangeHistoryRepository = get(),
        )
    }

    single<FavoriteCurrenciesRepository> {
        FixedFavoriteCurrenciesRepository(
            currencies = listOf(
                Currency.EUR,
                Currency.USD,
            )
        )
    }

    viewModel {
        OverviewViewModel(
            availableCurrenciesRepository = get(),
            exchangeCurrenciesRule = get(),
            exchangeFeesRule = get(),
            balancesRepository = get(),
        )
    }
}