package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow

/**
 * Provides continuous updates on available currencies.
 */
interface CurrenciesRepository {

    fun currencies(): Flow<Set<Currency>>
}
