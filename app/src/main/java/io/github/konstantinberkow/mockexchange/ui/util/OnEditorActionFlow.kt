package io.github.konstantinberkow.mockexchange.ui.util

import android.view.KeyEvent
import android.widget.TextView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

fun TextView.editorActionEvents(handler: (Int, KeyEvent?) -> Boolean) : Flow<String> = callbackFlow {
    setOnEditorActionListener { sender, actionId, event ->
        val handled = handler(actionId, event)
        if (handled) {
            trySend(sender.text.toString())
        }
        return@setOnEditorActionListener handled
    }

    awaitClose {
        setOnEditorActionListener(null)
    }
}.conflate()