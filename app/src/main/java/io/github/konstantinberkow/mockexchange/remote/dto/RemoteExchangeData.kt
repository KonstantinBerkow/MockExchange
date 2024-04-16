package io.github.konstantinberkow.mockexchange.remote.dto

import io.github.konstantinberkow.mockexchange.entity.Currency

data class RemoteExchangeData(
    val base: Currency,
    val exchangeRates: Map<Currency, Double>
)
