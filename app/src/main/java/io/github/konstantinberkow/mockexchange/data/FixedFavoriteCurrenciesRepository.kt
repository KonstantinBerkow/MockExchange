package io.github.konstantinberkow.mockexchange.data

import io.github.konstantinberkow.mockexchange.entity.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FixedFavoriteCurrenciesRepository(
    private val favoriteCurrencies: LinkedHashSet<Currency>
) : FavoriteCurrenciesRepository {

    constructor(currencies: List<Currency>) : this(
        favoriteCurrencies = currencies.mapTo(linkedSetOf()) { it }
    )

    override fun currencies(): Flow<Set<Currency>> {
        return flowOf(favoriteCurrencies)
    }
}
