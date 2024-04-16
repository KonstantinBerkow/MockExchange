package io.github.konstantinberkow.mockexchange.remote.serialization

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import io.github.konstantinberkow.mockexchange.entity.Currency
import io.github.konstantinberkow.mockexchange.remote.dto.RemoteExchangeData

private const val BASE_KEY = "base"
private const val RATES_KEY = "rates"

class RemoteExchangeDataGsonTypeAdapter : TypeAdapter<RemoteExchangeData>() {

    // No need to support write, it's not like we need it,
    // but JsonDeserializer does not perform operate in streaming mode
    // while TypeAdapter does
    override fun write(out: JsonWriter, value: RemoteExchangeData) {
        TODO("Not supported")
    }

    override fun read(input: JsonReader): RemoteExchangeData {
        var baseCurrency: Currency? = null
        var knownRates: Map<Currency, Double> = emptyMap()

        input.beginObject()
        while (input.peek() != JsonToken.END_OBJECT) {
            when (input.nextName()) {
                BASE_KEY ->
                    baseCurrency = input.nextString()?.toCurrency()

                RATES_KEY ->
                    knownRates = mutableMapOf<Currency, Double>().apply {
                        input.beginObject()
                        while (input.hasNext()) {
                            val currency = input.nextName().toCurrency()
                            val exchangeRate = input.parseExchangeRate()
                            put(currency, exchangeRate)
                        }
                        input.endObject()
                    }

                else -> input.skipValue()
            }
        }
        input.endObject()

        require(baseCurrency != null) {
            "Failed to parse exchange data - $BASE_KEY not specified!"
        }
        require(knownRates.isNotEmpty()) {
            "Failed to parse exchange data - $RATES_KEY not specified or empty!"
        }

        return RemoteExchangeData(
            base = baseCurrency,
            exchangeRates = knownRates
        )
    }
}

private fun JsonReader.parseExchangeRate(): Double {
    val rawValue = nextDouble()
    require(rawValue.isFinite() && rawValue > 0) {
        "'$rawValue' is not a valid exchange rate!"
    }
    return rawValue
}

private fun String.toCurrency(): Currency {
    require(isNotBlank()) {
        "Can't create currency from identifier: '$this'!"
    }
    return Currency(identifier = this)
}
