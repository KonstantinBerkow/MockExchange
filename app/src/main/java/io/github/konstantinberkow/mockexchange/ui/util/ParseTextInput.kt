package io.github.konstantinberkow.mockexchange.ui.util

import java.text.NumberFormat
import java.text.ParseException

object ParseTextInput : suspend (String) -> UInt? {

    override suspend fun invoke(text: String): UInt? {
        return try {
            NumberFormat.getNumberInstance().parse(text)?.let {
                (it.toDouble() * 100).toUInt()
            }
        } catch (e: ParseException) {
            null
        }
    }
}
