package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow

interface FavoriteCurrenciesRepository {

    fun currencies(): Flow<Set<Currency>>
}
