package io.github.konstantinberkow.mockexchange.remote

import io.github.konstantinberkow.mockexchange.remote.dto.RemoteExchangeData
import retrofit2.http.GET

interface ExchangeRatesApi {

    @GET("currency-exchange-rates")
    suspend fun getExchangeRates(): RemoteExchangeData
}