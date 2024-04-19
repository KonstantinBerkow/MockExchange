package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow

interface ExchangeHistoryRepository {

    suspend fun record(discharge: UInt, source: Currency, addition: UInt, target: Currency)

    fun exchangesCount(): Flow<UInt>
}
