package io.github.konstantinberkow.mockexchange.entity.exchange_rule

import io.github.konstantinberkow.mockexchange.entity.Currency

/**
 * Defines a rule to calculate fee (if any) applicable to discharge from account.
 */
interface DischargeFeeUseCase<FundsMeasure : Any> {

    suspend fun calculateFeeForDischarge(
        amount: FundsMeasure,
        currency: Currency,
    ): Result<FundsMeasure>

    sealed interface Result<out T> {

        data class Fee<out T>(val amount: T) : Result<T>

        data object Free : Result<Nothing>
    }
}
