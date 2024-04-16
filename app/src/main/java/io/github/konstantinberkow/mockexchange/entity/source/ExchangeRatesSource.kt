package io.github.konstantinberkow.mockexchange.entity.source

import io.github.konstantinberkow.mockexchange.entity.rates.ExchangeRates
import kotlinx.coroutines.flow.Flow

interface ExchangeRatesSource {

    fun exchangeRates(): Flow<ExchangeRates>
}