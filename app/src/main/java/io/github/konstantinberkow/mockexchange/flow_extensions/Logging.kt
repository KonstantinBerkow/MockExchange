package io.github.konstantinberkow.mockexchange.flow_extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

fun <T> Flow<T>.logError(tag: String, msgTemplate: String, vararg args: Any) =
    catch { error ->
        Timber.tag(tag).e(error, msgTemplate, args)
    }

fun <T> Flow<T>.logEach(tag: String, msgTemplate: String) =
    onEach {
        Timber.tag(tag).d(msgTemplate, it)
    }

fun <T> Flow<T>.logEach(tag: String, msgProvider: (T) -> String) =
    onEach {
        Timber.tag(tag).d("%s", LazyToString(it, msgProvider))
    }

class LazyToString<T>(
    private val data: T,
    private val msgProvider: (T) -> String
) {

    override fun toString(): String {
        return msgProvider(data)
    }
}
