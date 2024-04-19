package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class InMemoryExchangeHistoryRepository(
    initialExchangesCount: UInt
) : ExchangeHistoryRepository {

    private val exchangesCountStateFlow = MutableStateFlow(
        value = initialExchangesCount
    )

    override suspend fun record(
        discharge: UInt,
        source: Currency,
        addition: UInt,
        target: Currency
    ) {
        exchangesCountStateFlow.update {
            it + 1u
        }
    }

    override fun exchangesCount(): Flow<UInt> {
        return exchangesCountStateFlow
    }
}