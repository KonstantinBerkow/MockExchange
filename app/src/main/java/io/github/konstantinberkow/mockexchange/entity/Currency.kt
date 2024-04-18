package io.github.konstantinberkow.mockexchange.entity

data class Currency(val identifier: String) {

    companion object {

        val EUR = Currency("EUR")

        val USD = Currency("USD")
    }
}
