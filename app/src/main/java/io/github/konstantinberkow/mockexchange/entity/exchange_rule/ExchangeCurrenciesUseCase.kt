package io.github.konstantinberkow.mockexchange.entity.exchange_rule

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow

/**
 * Defines a rule to convert between two currencies.
 */
interface ExchangeCurrenciesUseCase<FundsMeasure : Any, out ConversionError : Any> {

    /**
     * Calculate amount of funds to discharge from account in [from] currency
     * to purchase [amount] funds in [to] currency.
     *
     * @param amount specifies desired amount of funds to purchase
     * @param to denotes desired currency
     * @param from denotes currency of result
     * @return [amount] converted from currency [to] into currency [from] using exchange rules
     */
    suspend fun convert(
        amount: FundsMeasure,
        to: Currency,
        from: Currency,
    ): Result<FundsMeasure, ConversionError>

    sealed interface Result<out T : Any, out E : Any> {

        data class Success<out T : Any>(val amountToDischarge: T) : Result<T, Nothing>

        data class Failure<out E : Any>(val reason: E) : Result<Nothing, E>
    }
}
