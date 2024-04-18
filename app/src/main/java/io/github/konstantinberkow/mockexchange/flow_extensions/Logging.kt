package io.github.konstantinberkow.mockexchange.flow_extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber

fun <T> Flow<T>.logError(tag: String, msgTemplate: String, vararg args: Any) =
    catch { error ->
        Timber.tag(tag).e(error, msgTemplate, args)
    }
