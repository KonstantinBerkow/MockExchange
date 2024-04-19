package io.github.konstantinberkow.mockexchange.remote.source

import io.github.konstantinberkow.mockexchange.remote.ExchangeRatesApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import kotlin.time.Duration

private const val TAG = "NetworkExchangeRatesSource"

class NetworkExchangeRatesSource(
    api: ExchangeRatesApi,
    dispatcher: CoroutineDispatcher,
    refreshDelay: Duration,
    shareScope: CoroutineScope,
    sharingTime: Long = 5000
) : ExchangeRatesSource {

    override val exchangeRates =
        flow {
            while (true) {
                val remoteData = try {
                    withContext(dispatcher) {
                        api.getExchangeRates()
                    }
                } catch (httpError: HttpException) {
                    Timber.tag(TAG).e(httpError, "Network request failed")
                    null
                } catch (ioError: IOException) {
                    Timber.tag(TAG).e(ioError, "Network request failed")
                    null
                }
                remoteData?.let {
                    emit(it)
                }
                delay(refreshDelay)
            }
        }
            .distinctUntilChanged()
            .shareIn(
                scope = shareScope,
                started = SharingStarted.WhileSubscribed(sharingTime),
                replay = 1
            )
}
