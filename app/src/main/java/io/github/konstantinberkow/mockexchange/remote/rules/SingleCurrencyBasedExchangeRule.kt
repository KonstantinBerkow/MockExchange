package io.github.konstantinberkow.mockexchange.remote.rules

import io.github.konstantinberkow.mockexchange.entity.Currency
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.ExchangeCurrenciesUseCase
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.ExchangeError

class SingleCurrencyBasedExchangeRule(
    private val base: Currency,
    private val knownRates: Map<Currency, Double>
) : ExchangeCurrenciesUseCase<UInt, ExchangeError> {

    override suspend fun convert(
        amount: UInt,
        to: Currency,
        from: Currency
    ): ExchangeCurrenciesUseCase.Result<UInt, ExchangeError> {
        // identity conversion
        if (from == to) {
            return ExchangeCurrenciesUseCase.Result.Failure(ExchangeError.IllegalConversion)
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
            return ExchangeCurrenciesUseCase.Result.Failure(
                ExchangeError.NoConversionBetweenCurrencies(
                    source = from,
                    target = to
                )
            )
        }
        val ratio = knownRates[ratioKey]
        requireNotNull(ratio) {
            "No conversion specified from '${from.identifier}' to '${to.identifier}'"
        }

        val amountInCents = amount.toDouble()
        val convertedInCents = if (performMultiplication) {
            amountInCents * ratio
        } else {
            amountInCents / ratio
        }
        require(convertedInCents.isFinite() && convertedInCents >= 0) {
            "Conversion failed, got: '${convertedInCents}' in cents!"
        }

        return convertedInCents.toUInt().let {
            ExchangeCurrenciesUseCase.Result.Success(
                amountToDischarge = it
            )
        }
    }
}