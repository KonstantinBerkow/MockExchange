package io.github.konstantinberkow.mockexchange.ui.util

import android.text.Editable
import android.text.TextUtils
import timber.log.Timber
import java.text.DecimalFormatSymbols

private const val TAG = "DecimalMaskTextWatcher"

class DecimalMaskTextWatcher : TextWatcherAdapter() {

    private var decimalSeparator: Char = '\u0000'

    private fun getDecimalSeparator(): Char {
        val oldSeparator = decimalSeparator
        return if (oldSeparator != '\u0000') {
            oldSeparator
        } else {
            DecimalFormatSymbols.getInstance().decimalSeparator.also {
                decimalSeparator = it
            }
        }
    }

    override fun afterTextChanged(edited: Editable) {
        val totalLength = edited.length
        Timber.tag(TAG).d("afterTextChanged edited: \"%s\", length: %d", edited, totalLength)
        if (totalLength <= 1) {
            Timber.tag(TAG).d("Early return")
            return
        }

        val separator = getDecimalSeparator()
        val indexOfSeparator = TextUtils.indexOf(edited, separator)
        Timber.tag(TAG).d("index of separator '%c' = %d", separator, indexOfSeparator)
        val shrinkedDecimalPart = shrinkDecimalPart(edited, indexOfSeparator, totalLength)
        if (shrinkedDecimalPart) {
            return
        }

        val indexOfNonZero = edited.indexOfFirst { it != '0' }
        Timber.tag(TAG).d("index of non zero: %d", indexOfNonZero)

        if (indexOfNonZero == indexOfSeparator) {
            if (indexOfSeparator == 0) {
                Timber.tag(TAG).d("Starts with decimal separator, add 0. insert at 0")
                edited.insert(0, "0")
            } else {
                Timber.tag(TAG).d("Nothing to do")
            }
        } else if (indexOfNonZero < 0) {
            Timber.tag(TAG)
                .d("No non 0 in string, leave one 0. delete [%d; %d)", 0, totalLength - 1)
            edited.delete(0, totalLength - 1)
        } else if (indexOfNonZero == 0) {
            Timber.tag(TAG).d("Starts with non 0, nothing to do")
        } else {
            Timber.tag(TAG).d("Remove leading zeroes. delete [%d; %d)", 0, indexOfNonZero)
            edited.delete(0, indexOfNonZero)
        }
    }

    private fun shrinkDecimalPart(
        edited: Editable,
        indexOfSeparator: Int,
        totalLength: Int
    ): Boolean {
        return if (indexOfSeparator < 0 || totalLength - 3 <= indexOfSeparator) {
            Timber.tag(TAG).d("Don't shrink decimal part")
            false
        } else {
            val deleteFrom = indexOfSeparator + 3
            Timber.tag(TAG).d("Delete [%d; %d)", deleteFrom, totalLength)
            edited.delete(deleteFrom, totalLength)
            true
        }
    }
}