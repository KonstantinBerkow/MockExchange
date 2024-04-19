package io.github.konstantinberkow.mockexchange.remote.source

import io.github.konstantinberkow.mockexchange.remote.ExchangeRatesApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlin.time.Duration

class NetworkExchangeRatesSource(
    api: ExchangeRatesApi,
    dispatcher: CoroutineDispatcher,
    refreshDelay: Duration,
    shareScope: CoroutineScope,
    sharingTime: Long = 5000
) : ExchangeRatesSource {

    override val exchangeRates =
        flow {
            while (true) {
                val remoteData = withContext(dispatcher) {
                    api.getExchangeRates()
                }
                emit(remoteData)
                delay(refreshDelay)
            }
        }
            .distinctUntilChanged()
            .shareIn(
                scope = shareScope,
                started = SharingStarted.WhileSubscribed(sharingTime),
                replay = 1
            )
}
