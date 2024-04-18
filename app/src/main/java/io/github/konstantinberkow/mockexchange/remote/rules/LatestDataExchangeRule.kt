package io.github.konstantinberkow.mockexchange.remote.rules

import io.github.konstantinberkow.mockexchange.entity.Currency
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.ExchangeCurrenciesUseCase
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.ExchangeError
import io.github.konstantinberkow.mockexchange.remote.source.ExchangeRatesSource
import kotlinx.coroutines.flow.first

class LatestDataExchangeRule(
    private val exchangeRatesSource: ExchangeRatesSource,
    private val ruleFromData: (Currency, Map<Currency, Double>) -> ExchangeCurrenciesUseCase<UInt, ExchangeError>
) : ExchangeCurrenciesUseCase<UInt, ExchangeError> {

    override suspend fun convert(
        amount: UInt,
        to: Currency,
        from: Currency
    ): ExchangeCurrenciesUseCase.Result<UInt, ExchangeError> {
        val data = exchangeRatesSource.exchangeRates.first()
        val rule = ruleFromData(data.base, data.exchangeRates)
        return rule.convert(amount, to, from)
    }
}
