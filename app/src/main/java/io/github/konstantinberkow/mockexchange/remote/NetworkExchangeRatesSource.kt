package io.github.konstantinberkow.mockexchange.remote

import io.github.konstantinberkow.mockexchange.entity.rates.ExchangeRates
import io.github.konstantinberkow.mockexchange.entity.source.ExchangeRatesSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Duration

class NetworkExchangeRatesSource(
    private val api: ExchangeRatesApi,
    private val dispatcher: CoroutineDispatcher,
    private val refreshDelay: Duration
) : ExchangeRatesSource {

    override fun exchangeRates(): Flow<ExchangeRates> {
        return flow {
            while (true) {
                val remoteData = withContext(dispatcher) {
                    api.getExchangeRates()
                }
                emit(remoteData)
                delay(refreshDelay)
            }
        }
            .distinctUntilChanged()
            .map {
                SingleCurrencyBasedStaticExchangeRates(
                    base = it.base,
                    knownRates = it.exchangeRates
                )
            }
    }
}
