package io.github.konstantinberkow.mockexchange.remote.data

import io.github.konstantinberkow.mockexchange.data.CurrenciesRepository
import io.github.konstantinberkow.mockexchange.entity.Currency
import io.github.konstantinberkow.mockexchange.remote.source.ExchangeRatesSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NetworkCurrenciesRepository(
    private val exchangeRatesSource: ExchangeRatesSource
) : CurrenciesRepository {

    override fun currencies(): Flow<Set<Currency>> {
        return exchangeRatesSource.exchangeRates
            .map {
                linkedSetOf<Currency>().apply {
                    add(it.base)
                    addAll(it.exchangeRates.keys)
                }
            }
    }
}
