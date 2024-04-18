package io.github.konstantinberkow.mockexchange.remote.source

import io.github.konstantinberkow.mockexchange.remote.dto.RemoteExchangeData
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ExchangeRatesSource {

    val exchangeRates: SharedFlow<RemoteExchangeData>
}
