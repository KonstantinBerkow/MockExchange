package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Balance
import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryBalancesRepository(
    initialBalances: LinkedHashMap<Currency, UInt>
) : UserBalancesRepository {

    private val lock = Mutex()

    private val balancesChannel = MutableStateFlow(
        value = initialBalances
    )

    override fun allBalances(): Flow<Set<Balance>> {
        return balancesChannel
            .map {
                it.mapTo(linkedSetOf()) { (currency, balance) ->
                    Balance(currency, balance)
                }
            }
    }

    override suspend fun performExchange(
        discharge: UInt,
        source: Currency,
        addition: UInt,
        target: Currency
    ) = lock.withLock {
        val latestMap = balancesChannel.value
        val sourceBalance = latestMap[source]
        require(sourceBalance != null && sourceBalance >= discharge) {
            "Not enough funds in account of $source, required: $discharge, was: $sourceBalance"
        }
        latestMap[source] = sourceBalance - discharge

        val oldTarget = latestMap[target] ?: 0u
        latestMap[target] = oldTarget + addition

        balancesChannel.emit(latestMap)
    }
}