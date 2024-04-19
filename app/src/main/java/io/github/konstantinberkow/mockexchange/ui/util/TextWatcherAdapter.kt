package io.github.konstantinberkow.mockexchange.ui.util

import android.text.Editable
import android.text.TextWatcher

open class TextWatcherAdapter : TextWatcher {

    override fun beforeTextChanged(preChanged: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(changed: CharSequence, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(edited: Editable) {
    }
}