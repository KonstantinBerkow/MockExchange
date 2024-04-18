package io.github.konstantinberkow.mockexchange.entity.exchange_rule

import io.github.konstantinberkow.mockexchange.entity.Currency

sealed interface ExchangeError {

    data object IllegalConversion : ExchangeError

    data class NoConversionBetweenCurrencies(
        val source: Currency,
        val target: Currency
    ) : ExchangeError
}