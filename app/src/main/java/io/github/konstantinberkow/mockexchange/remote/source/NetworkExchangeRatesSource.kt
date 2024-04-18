package io.github.konstantinberkow.mockexchange.remote.source

import io.github.konstantinberkow.mockexchange.remote.ExchangeRatesApi
import io.github.konstantinberkow.mockexchange.remote.dto.RemoteExchangeData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlin.time.Duration

@OptIn(DelicateCoroutinesApi::class)
class NetworkExchangeRatesSource(
    api: ExchangeRatesApi,
    dispatcher: CoroutineDispatcher,
    refreshDelay: Duration,
    shareScope: CoroutineScope = GlobalScope,
    sharingTime: Long = 5000
) : ExchangeRatesSource {

    private val ratesChannel = Channel<RemoteExchangeData>(
        capacity = Channel.CONFLATED
    )

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
