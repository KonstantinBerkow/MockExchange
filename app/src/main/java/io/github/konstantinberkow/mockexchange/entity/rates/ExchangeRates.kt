package io.github.konstantinberkow.mockexchange.entity.rates

import io.github.konstantinberkow.mockexchange.entity.Currency

interface ExchangeRates {

    fun currencyForIdentifier(identifier: String): Currency?

    fun convert(amount: UInt, from: Currency, to: Currency): UInt
}
