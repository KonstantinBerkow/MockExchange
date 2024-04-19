package io.github.konstantinberkow.mockexchange.ui.util

import android.util.Log
import android.view.View
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

fun View.clicks(): Flow<Unit> =
    callbackFlow {
        setOnClickListener {
            trySend(Unit)
        }
        awaitClose {
            setOnClickListener(null)
        }
    }
        .conflate()
