package io.github.konstantinberkow.mockexchange.entity.exchange_rule

import io.github.konstantinberkow.mockexchange.entity.Currency

class StaticFeeUseCase(
    private val percent: Float,
) : DischargeFeeUseCase<UInt> {

    override suspend fun calculateFeeForDischarge(
        amount: UInt,
        currency: Currency
    ): DischargeFeeUseCase.Result<UInt> {
        return (amount.toDouble() * percent).toUInt().let {
            DischargeFeeUseCase.Result.Fee(it)
        }
    }
}