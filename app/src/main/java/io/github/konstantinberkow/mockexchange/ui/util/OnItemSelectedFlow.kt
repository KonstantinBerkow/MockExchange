package io.github.konstantinberkow.mockexchange.ui.util

import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onStart

fun AdapterView<*>.onItemSelectedFlow(): Flow<AdapterViewSelectionEvent> {
    val selectedPosition = selectedItemPosition
    val initialEvent = if (selectedPosition == AdapterView.INVALID_POSITION) {
        AdapterViewSelectionEvent.NothingSelected(sender = this)
    } else {
        AdapterViewSelectionEvent.ItemSelected(
            sender = this,
            selectedView = selectedView,
            position = selectedPosition,
            id = selectedItemId
        )
    }
    return callbackFlow {
        val listener = object : OnItemSelectedListener {

            override fun onItemSelected(
                sender: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                trySend(
                    AdapterViewSelectionEvent.ItemSelected(
                        sender = sender,
                        selectedView = view,
                        position = position,
                        id = id,
                    )
                )
            }

            override fun onNothingSelected(sender: AdapterView<*>) {
                trySend(
                    AdapterViewSelectionEvent.NothingSelected(
                        sender = sender,
                    )
                )
            }
        }
        onItemSelectedListener = listener
        awaitClose {
            onItemSelectedListener = null
        }
    }
        .conflate()
        .onStart {
            emit(initialEvent)
        }
}


sealed interface AdapterViewSelectionEvent {

    val sender: AdapterView<*>

    val selectedValue: Any?

    class ItemSelected(
        override val sender: AdapterView<*>,
        val selectedView: View?,
        val position: Int,
        val id: Long
    ) : AdapterViewSelectionEvent {

        override val selectedValue: Any?
            get() = sender.selectedItem
    }

    class NothingSelected(
        override val sender: AdapterView<*>
    ) : AdapterViewSelectionEvent {

        override val selectedValue: Any?
            get() = null
    }
}
