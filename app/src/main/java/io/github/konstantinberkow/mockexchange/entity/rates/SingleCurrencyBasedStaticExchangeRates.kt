package io.github.konstantinberkow.mockexchange.entity.rates

import io.github.konstantinberkow.mockexchange.entity.Currency

class SingleCurrencyBasedStaticExchangeRates(
    private val base: Currency,
    private val knownRates: Map<Currency, Float>
) : ExchangeRates {

    override fun currencyForIdentifier(identifier: String): Currency? {
        if (identifier == base.identifier) {
            return base
        }
        return knownRates.keys.firstOrNull { identifier == it.identifier }
    }

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

        val ratio = if (from == base) {
            knownRates[to]
        } else if (to == base) {
            knownRates[to]?.let {
                1 / it
            }
        } else {
            // Example: we only know how to convert to and from EUR
            // we don't know how to convert from UAH to USD
            null
        }
        requireNotNull(ratio) {
            "No conversion specified from '${from.identifier}' to '${to.identifier}'"
        }

        val amountInCents = amount * 100u
        val convertedInCents = amountInCents.toDouble() * ratio
        require(convertedInCents.isFinite() && convertedInCents >= 0) {
            "Conversion failed, got: '${convertedInCents}' in cents!"
        }

        return (convertedInCents / 100).toUInt()
    }
}
