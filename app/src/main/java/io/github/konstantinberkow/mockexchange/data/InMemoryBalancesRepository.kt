package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class InMemoryBalancesRepository(
    initialBalances: Map<Currency, UInt>
) : UserBalancesRepository {

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
    ) {
        balancesStateFlow.update { latestMap ->
            val sourceBalance = latestMap[source]
            require(sourceBalance != null && sourceBalance >= discharge) {
                "Not enough funds in account of $source, required: $discharge, was: $sourceBalance"
            }

            val copy = latestMap.toMutableMap()

            copy[source] = sourceBalance - discharge

            val oldTarget = latestMap[target] ?: 0u
            copy[target] = oldTarget + addition

            copy
        }
    }
}