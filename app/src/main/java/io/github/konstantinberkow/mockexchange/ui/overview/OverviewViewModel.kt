package io.github.konstantinberkow.mockexchange.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.konstantinberkow.mockexchange.data.CurrenciesRepository
import io.github.konstantinberkow.mockexchange.data.UserBalancesRepository
import io.github.konstantinberkow.mockexchange.entity.Balance
import io.github.konstantinberkow.mockexchange.entity.Currency
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.DischargeFeeUseCase
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.ExchangeCurrenciesUseCase
import io.github.konstantinberkow.mockexchange.entity.exchange_rule.ExchangeError
import io.github.konstantinberkow.mockexchange.flow_extensions.ToPair
import io.github.konstantinberkow.mockexchange.flow_extensions.logError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "OverviewViewModel"

class OverviewViewModel(
    private val availableCurrenciesRepository: CurrenciesRepository,
    private val exchangeCurrenciesRule: ExchangeCurrenciesUseCase<UInt, ExchangeError>,
    private val exchangeFeesRule: DischargeFeeUseCase<UInt>,
    private val balancesRepository: UserBalancesRepository,
) : ViewModel() {

    private val actions = Channel<Action>(capacity = Channel.RENDEZVOUS)

    private val singleTimeResults = Channel<Result.SingleTime>(capacity = Channel.RENDEZVOUS)

    fun singleTimeEvents(): Flow<Result.SingleTime> {
        return singleTimeResults.receiveAsFlow()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val uiState =
        actions.consumeAsFlow()
            .combine(balancesRepository.allBalances().distinctUntilChanged(), ToPair())
            .flatMapLatest { (action, allBalances) ->
                when (action) {
                    Action.Load ->
                        performLoad(allBalances)

                    is Action.EstimateExchange ->
                        estimateExchange(action)

                    is Action.CommitExchange ->
                        commitExchange(allBalances, action)
                }
            }
            .scan<Result, UiState>(UiState.Loading) { oldState, result ->
                if (result is Result.SingleTime) {
                    singleTimeResults.send(result)
                }
                oldState.mergeWithResult(result)
            }
            .catch {
                Timber.tag(TAG).e(it, "Failed combining actions into state")
                emit(UiState.LoadFailed)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = UiState.Loading
            )

    private fun performLoad(userBalances: Set<Balance>): Flow<Result> {
        return availableCurrenciesRepository.currencies()
            .distinctUntilChanged()
            .logError(TAG, "Failed loading available currencies!")
            .map<Set<Currency>, Result> { currenciesSet ->
                // sort with EUR and USD coming first
                val allCurrencies = mutableListOf<Currency>().apply {
                    add(Currency.EUR)
                    add(Currency.USD)
                }
                currenciesSet.mapNotNullTo(allCurrencies) {
                    it.takeIf { it != Currency.EUR && it != Currency.USD }
                }
                val balancesForAvailableCurrencies = allCurrencies.map { currency ->
                    Balance(
                        currency = currency,
                        amount = userBalances.firstOrNull { it.currency == currency }?.amount ?: 0u
                    )
                }
                Result.Loaded(
                    currencies = allCurrencies,
                    balances = balancesForAvailableCurrencies
                )
            }
            .onStart {
                emit(Result.Loading)
            }
            .catch {
                Timber.tag(TAG).e(it, "Failed to load data for overview")
                emit(Result.LoadFailed)
            }
    }

    private suspend fun estimateExchange(action: Action.EstimateExchange): Flow<Result> {
        val requestedAmount = action.amount
        val requestCurrency = action.target
        val sourceCurrency = action.source
        val conversion = exchangeCurrenciesRule.convert(
            amount = requestedAmount,
            to = requestCurrency,
            from = sourceCurrency,
        )
        val result = when (conversion) {
            is ExchangeCurrenciesUseCase.Result.Success -> {
                val dischargeAmount = conversion.amountToDischarge
                val feeResult = exchangeFeesRule.calculateFeeForDischarge(
                    amount = dischargeAmount,
                    currency = sourceCurrency
                )
                val fee = when (feeResult) {
                    is DischargeFeeUseCase.Result.Fee -> feeResult.amount
                    DischargeFeeUseCase.Result.Free -> 0u
                }
                Result.ExchangeEstimation(
                    amount = dischargeAmount,
                    fee = fee,
                    from = sourceCurrency,
                    converted = requestedAmount,
                    to = requestCurrency,
                )
            }

            is ExchangeCurrenciesUseCase.Result.Failure -> {
                when (conversion.reason) {
                    is ExchangeError.NoConversionBetweenCurrencies,
                    ExchangeError.IllegalConversion ->
                        Result.NoConversionAvailable(
                            source = sourceCurrency,
                            target = requestCurrency,
                        )
                }
            }
        }
        return flowOf(result)
    }

    private fun commitExchange(
        allBalances: Set<Balance>,
        action: Action.CommitExchange
    ): Flow<Result> {
        val discharge = action.discharge
        val sourceCurrency = action.source
        val sourceBalance = allBalances.firstOrNull { it.currency == sourceCurrency }
        val balanceAmount = sourceBalance?.amount ?: 0u
        return if (balanceAmount < discharge) {
            flowOf(
                Result.ExchangeFailure.NotEnoughFunds(exchange = action)
            )
        } else {
            flow {
                emit(Result.ExchangeStarted)
                balancesRepository.performExchange(
                    discharge = discharge,
                    source = sourceCurrency,
                    addition = action.requestAmount,
                    target = action.target
                )
                emit(Result.ExchangeSuccess)
            }
                .catch {
                    Timber.tag(TAG).e(it, "Exchange failed: %s", action)
                    emit(Result.ExchangeFailure.RemoteError)
                }
        }
    }

    fun state(): Flow<UiState> {
        return uiState
    }

    fun accept(action: Action) {
        viewModelScope.launch {
            actions.send(action)
        }
    }

    override fun onCleared() {
        super.onCleared()
        actions.close()
        singleTimeResults.close()
    }

    sealed interface UiState {

        fun mergeWithResult(result: Result): UiState

        data object Loading : UiState {

            override fun mergeWithResult(result: Result): UiState {
                return when (result) {
                    Result.Loading,
                    Result.LoadFailed,
                    is Result.SingleTime,
                    Result.ExchangeStarted,
                    Result.ExchangeSuccess -> this

                    is Result.Loaded -> Loaded(
                        availableCurrencies = result.currencies,
                        userFunds = result.balances,
                        performingExchange = false
                    )
                }
            }
        }

        data object LoadFailed : UiState {

            override fun mergeWithResult(result: Result): UiState {
                return when (result) {
                    Result.Loading -> Loading

                    Result.LoadFailed,
                    is Result.SingleTime,
                    Result.ExchangeStarted,
                    Result.ExchangeSuccess -> this

                    is Result.Loaded -> Loaded(
                        availableCurrencies = result.currencies,
                        userFunds = result.balances,
                        performingExchange = false
                    )
                }
            }
        }

        /**
         * @param availableCurrencies [List] of supported currencies
         * @param userFunds [List] of user's balances
         */
        data class Loaded(
            val availableCurrencies: List<Currency>,
            val userFunds: List<Balance>,
            val performingExchange: Boolean
        ) : UiState {

            override fun mergeWithResult(result: Result): UiState {
                return when (result) {
                    Result.LoadFailed -> LoadFailed

                    Result.Loading,
                    is Result.ExchangeEstimation,
                    is Result.NoConversionAvailable -> this

                    Result.ExchangeStarted ->
                        copy(
                            performingExchange = true
                        )

                    is Result.ExchangeFailure,
                    Result.ExchangeSuccess ->
                        copy(
                            performingExchange = false
                        )

                    is Result.Loaded -> Loaded(
                        availableCurrencies = result.currencies,
                        userFunds = result.balances,
                        performingExchange = performingExchange
                    )
                }
            }
        }
    }

    sealed interface Action {

        data object Load : Action

        data class EstimateExchange(
            val amount: UInt,
            val source: Currency,
            val target: Currency,
        ) : Action

        data class CommitExchange(
            val discharge: UInt,
            val source: Currency,
            val requestAmount: UInt,
            val target: Currency,
        ) : Action
    }

    sealed interface Result {

        data object Loading : Result

        data object LoadFailed : Result

        data class Loaded(
            val currencies: List<Currency>,
            val balances: List<Balance>
        ) : Result

        sealed interface SingleTime : Result

        /**
         * @param amount how much money wee want to convert, in [from] currency
         * @param fee fee (if applicable) for this conversion, in [from] currency
         * @param converted how much user will receive on another account in [to] currency
         */
        data class ExchangeEstimation(
            val amount: UInt,
            val fee: UInt,
            val from: Currency,
            val converted: UInt,
            val to: Currency,
        ) : SingleTime

        data class NoConversionAvailable(
            val source: Currency,
            val target: Currency
        ) : SingleTime

        data object ExchangeStarted : Result

        sealed interface ExchangeFailure : SingleTime {

            data object RemoteError : ExchangeFailure

            data class NotEnoughFunds(
                val exchange: Action.CommitExchange
            ) : ExchangeFailure
        }

        data object ExchangeSuccess : Result
    }
}
