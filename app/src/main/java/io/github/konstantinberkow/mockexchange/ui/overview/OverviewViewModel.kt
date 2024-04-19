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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val TAG = "OverviewViewModel"

class OverviewViewModel(
    private val availableCurrenciesRepository: CurrenciesRepository,
    private val exchangeCurrenciesRule: ExchangeCurrenciesUseCase<UInt, ExchangeError>,
    private val exchangeFeesRule: DischargeFeeUseCase<UInt>,
    private val balancesRepository: UserBalancesRepository,
) : ViewModel() {

    private val selectedSourceCurrency = MutableStateFlow(value = Currency.EUR)

    private val selectedTargetCurrency = MutableStateFlow(value = Currency.USD)

    private val dischargeAmount = MutableStateFlow<UInt?>(value = null)

    fun updateSourceCurrency(currency: Currency) {
        Timber.tag(TAG).d("updateSourceCurrency to %s", currency)
        viewModelScope.launch {
            selectedSourceCurrency.emit(currency)
        }
    }

    fun updateTargetCurrency(currency: Currency) {
        Timber.tag(TAG).d("updateTargetCurrency to %s", currency)
        viewModelScope.launch {
            selectedTargetCurrency.emit(currency)
        }
    }

    fun updateDischargeAmount(amount: UInt?) {
        Timber.tag(TAG).d("updateDischargeAmount to %s", amount)
        dischargeAmount.update { amount }
    }

    private val oneShotEvents = Channel<Event>(capacity = Channel.RENDEZVOUS)

    fun oneShotEvents(): Flow<Event> {
        return oneShotEvents.receiveAsFlow()
    }

    val uiState = combine(
        availableCurrenciesRepository.currencies().distinctUntilChanged(),
        balancesRepository.allBalances().distinctUntilChanged(),
        selectedSourceCurrency,
        selectedTargetCurrency,
        dischargeAmount,
    ) { availableCurrencies, balances, sourceCurrency, targetCurrency, dischargeAmount ->
        // sort with EUR and USD coming first
        val allCurrenciesSorted = mutableListOf<Currency>().apply {
            add(Currency.EUR)
            add(Currency.USD)
            availableCurrencies.mapNotNullTo(this) {
                it.takeIf { it != Currency.EUR && it != Currency.USD }
            }
        }

        val estimatedTransfer = dischargeAmount?.let {
            val res = exchangeCurrenciesRule.convert(
                amount = it,
                to = targetCurrency,
                from = sourceCurrency
            )
            when (res) {
                is ExchangeCurrenciesUseCase.Result.Failure -> null
                is ExchangeCurrenciesUseCase.Result.Success -> res.amountToDischarge
            }
        }

        UiState(
            availableSourceCurrencies = allCurrenciesSorted - targetCurrency,
            availableTargetCurrencies = allCurrenciesSorted - sourceCurrency,
            balances = balances,
            selectedSourceCurrency = sourceCurrency,
            selectedTargetCurrency = targetCurrency,
            dischargeFromSource = dischargeAmount,
            targetTransfer = estimatedTransfer,
        )
    }
        .onEach { state ->
            Timber.tag(TAG).d("[UI_STATE]")
            Timber.tag(TAG).d(
                "Source currencies: %s and %d total",
                state.availableSourceCurrencies.take(5).map { it.identifier },
                state.availableSourceCurrencies.size
            )
            Timber.tag(TAG).d(
                "Target currencies: %s and %d total",
                state.availableTargetCurrencies.take(5).map { it.identifier },
                state.availableTargetCurrencies.size
            )
            Timber.tag(TAG).d("Selected source: %s", state.selectedSourceCurrency.identifier)
            Timber.tag(TAG).d("Selected target: %s", state.selectedTargetCurrency.identifier)
            Timber.tag(TAG).d("Discharge amount: %s", state.dischargeFromSource)
            Timber.tag(TAG).d("Target amount: %s", state.targetTransfer)
            Timber.tag(TAG).d("[UI_STATE]")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState(
                availableSourceCurrencies = emptyList(),
                availableTargetCurrencies = emptyList(),
                balances = emptyMap(),
                selectedSourceCurrency = Currency.EUR,
                selectedTargetCurrency = Currency.USD,
                dischargeFromSource = null,
                targetTransfer = null
            )
        )

    fun commitExchange(
        removeAmount: UInt,
        from: Currency,
        transferAmount: UInt,
        to: Currency
    ) {
        viewModelScope.launch {
            oneShotEvents.send(Event.ExchangeStarted)

            val feeResult = exchangeFeesRule.calculateFeeForDischarge(
                amount = removeAmount,
                currency = from
            )
            val feeAmount = when (feeResult) {
                is DischargeFeeUseCase.Result.Fee -> feeResult.amount
                DischargeFeeUseCase.Result.Free -> 0u
            }

            val res = balancesRepository.performExchange(
                discharge = removeAmount + feeAmount,
                source = from,
                addition = transferAmount,
                target = to
            )
            val transaction = Event.Transaction(
                amount = removeAmount,
                fee = feeAmount,
                from = from,
                converted = transferAmount,
                to = to,
            )
            val event = when (res) {
                UserBalancesRepository.Result.NotEnoughFunds ->
                    Event.NotEnoughFunds(transaction)

                UserBalancesRepository.Result.Success ->
                    Event.ExchangeSuccess(transaction)
            }
            oneShotEvents.send(event)
        }
    }

    data class UiState(
        val availableSourceCurrencies: List<Currency>,
        val availableTargetCurrencies: List<Currency>,
        val balances: Map<Currency, UInt>,
        val selectedSourceCurrency: Currency,
        val selectedTargetCurrency: Currency,
        val dischargeFromSource: UInt?,
        val targetTransfer: UInt?,
    ) {

        val loading: Boolean
            get() = balances.isEmpty()

        val balancesList: List<Balance> by lazy(mode = LazyThreadSafetyMode.NONE) {
            balances.map { Balance(it.key, it.value) }
        }
    }

    sealed interface Event {

        data object ExchangeStarted : Event

        data class NotEnoughFunds(
            val transaction: Transaction
        ) : Event

        data class ExchangeSuccess(
            val transaction: Transaction
        ) : Event

        data class Transaction(
            val amount: UInt,
            val fee: UInt,
            val from: Currency,
            val converted: UInt,
            val to: Currency,
        )
    }
}
