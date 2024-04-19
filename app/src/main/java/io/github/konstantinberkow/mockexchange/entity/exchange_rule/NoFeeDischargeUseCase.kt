package io.github.konstantinberkow.mockexchange.entity.exchange_rule

import io.github.konstantinberkow.mockexchange.entity.Currency

class NoFeeDischargeUseCase<T : Any> : DischargeFeeUseCase<T> {

    override suspend  fun calculateFeeForDischarge(
        amount: T,
        currency: Currency
    ): DischargeFeeUseCase.Result<T> {
        return DischargeFeeUseCase.Result.Free
    }
}
