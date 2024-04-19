package io.github.konstantinberkow.mockexchange.remote.source

import io.github.konstantinberkow.mockexchange.remote.dto.RemoteExchangeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class StartWithExchangeRatesSource(
    firstValue: RemoteExchangeData,
    delegate: ExchangeRatesSource,
    shareScope: CoroutineScope,
    replayTime: Long = 5000
) : ExchangeRatesSource {

    override val exchangeRates: Flow<RemoteExchangeData> = delegate.exchangeRates
        .stateIn(
            scope = shareScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = replayTime),
            initialValue = firstValue
        )
}
