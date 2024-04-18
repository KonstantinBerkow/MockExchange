package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Balance
import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow

interface UserBalancesRepository {

    fun allBalances(): Flow<Set<Balance>>

    suspend fun performExchange(
        discharge: UInt,
        source: Currency,
        addition: UInt,
        target: Currency
    )
}
