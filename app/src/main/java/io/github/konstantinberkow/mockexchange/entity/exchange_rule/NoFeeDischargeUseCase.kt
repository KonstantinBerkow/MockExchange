package io.github.konstantinberkow.mockexchange.entity.exchange_rule

import io.github.konstantinberkow.mockexchange.entity.Currency

class NoFeeDischargeUseCase : DischargeFeeUseCase<Any> {

    override suspend  fun calculateFeeForDischarge(
        amount: Any,
        currency: Currency
    ): DischargeFeeUseCase.Result<Any> {
        return DischargeFeeUseCase.Result.Free
    }
}
