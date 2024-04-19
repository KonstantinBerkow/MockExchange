package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow

interface UserBalancesRepository {

    fun allBalances(): Flow<Map<Currency, UInt>>

    suspend fun performExchange(
        discharge: UInt,
        source: Currency,
        addition: UInt,
        target: Currency
    ) : Result

    sealed interface Result {

        data object Success : Result

        data object NotEnoughFunds : Result
    }
}
