package io.github.konstantinberkow.mockexchange.entity.exchange_rule

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

@OptIn(DelicateCoroutinesApi::class)
class SwitchingDischargeFeeUseCase<T : Any, S : Any>(
    signal: Flow<S>,
    delegate: (S) -> DischargeFeeUseCase<T>,
    scope: CoroutineScope = GlobalScope,
    aliveTime: Long = 5000
) : DischargeFeeUseCase<T> {

    private val delegateFlow = signal
        .map { delegate(it) }
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(aliveTime),
            replay = 1
        )

    override suspend fun calculateFeeForDischarge(
        amount: T,
        currency: Currency
    ): DischargeFeeUseCase.Result<T> {
        return delegateFlow.first().calculateFeeForDischarge(amount, currency)
    }
}
