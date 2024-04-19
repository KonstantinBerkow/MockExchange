package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryBalancesRepository(
    initialBalances: Map<Currency, UInt>,
    private val exchangeHistoryRepository: ExchangeHistoryRepository
) : UserBalancesRepository {

    private val lock = Mutex()

    private val balancesStateFlow = MutableStateFlow(
        value = initialBalances
    )

    override fun allBalances(): Flow<Map<Currency, UInt>> {
        return balancesStateFlow
    }

    override suspend fun performExchange(
        discharge: UInt,
        source: Currency,
        addition: UInt,
        target: Currency
    ): UserBalancesRepository.Result = lock.withLock {
        val latestMap = balancesStateFlow.first()

        val sourceBalance = latestMap[source]
        if (sourceBalance == null || sourceBalance < discharge) {
            return@withLock UserBalancesRepository.Result.NotEnoughFunds
        }

        val copy = latestMap.toMutableMap()

        copy[source] = sourceBalance - discharge

        val oldTarget = latestMap[target] ?: 0u
        copy[target] = oldTarget + addition

        balancesStateFlow.emit(copy)

        exchangeHistoryRepository.record(
            discharge = discharge,
            source = source,
            addition = addition,
            target = target,
        )

        UserBalancesRepository.Result.Success
    }
}