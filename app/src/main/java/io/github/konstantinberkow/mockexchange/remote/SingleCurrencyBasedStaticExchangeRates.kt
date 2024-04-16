package io.github.konstantinberkow.mockexchange.remote

import io.github.konstantinberkow.mockexchange.entity.Currency
import io.github.konstantinberkow.mockexchange.entity.rates.ExchangeRates
import io.github.konstantinberkow.mockexchange.entity.toCents
import io.github.konstantinberkow.mockexchange.entity.toUnit

class SingleCurrencyBasedStaticExchangeRates(
    private val base: Currency,
    private val knownRates: Map<Currency, Double>
) : ExchangeRates {

    override val availableCurrencies: List<Currency> =
        knownRates.keys.toList()

    override fun convert(
        amount: UInt,
        from: Currency,
        to: Currency
    ): UInt {
        // specific case of identity conversion
        // not sure, perhaps exception is more fitting in such case
        if (from == to) {
            return amount
        }

        val ratioKey: Currency?
        val performMultiplication: Boolean
        if (from == base) {
            ratioKey = to
            performMultiplication = true
        } else if (to == base) {
            ratioKey = from
            performMultiplication = false
        } else {
            // Example: we only know how to convert to and from EUR
            // we don't know how to convert from UAH to USD
            ratioKey = null
            performMultiplication = false
        }
        val ratio = ratioKey?.let { knownRates[it] }
        requireNotNull(ratio) {
            "No conversion specified from '${from.identifier}' to '${to.identifier}'"
        }

        val amountInCents = amount.toCents().toDouble()
        val convertedInCents = if (performMultiplication) {
            amountInCents * ratio
        } else {
            amountInCents / ratio
        }
        require(convertedInCents.isFinite() && convertedInCents >= 0) {
            "Conversion failed, got: '${convertedInCents}' in cents!"
        }

        return convertedInCents.toUInt().toUnit()
    }
}
