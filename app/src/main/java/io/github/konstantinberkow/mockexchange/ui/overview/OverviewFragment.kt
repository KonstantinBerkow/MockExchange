package io.github.konstantinberkow.mockexchange.ui.overview

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import io.github.konstantinberkow.mockexchange.R
import io.github.konstantinberkow.mockexchange.databinding.FragmentOverviewBinding
import io.github.konstantinberkow.mockexchange.entity.Balance
import io.github.konstantinberkow.mockexchange.entity.Currency
import io.github.konstantinberkow.mockexchange.flow_extensions.logEach
import io.github.konstantinberkow.mockexchange.ui.util.AdapterViewSelectionEvent
import io.github.konstantinberkow.mockexchange.ui.util.DecimalMaskTextWatcher
import io.github.konstantinberkow.mockexchange.ui.util.ParseTextInput
import io.github.konstantinberkow.mockexchange.ui.util.clicks
import io.github.konstantinberkow.mockexchange.ui.util.editorActionEvents
import io.github.konstantinberkow.mockexchange.ui.util.hideSoftKeyboard
import io.github.konstantinberkow.mockexchange.ui.util.onItemSelectedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.text.NumberFormat

private const val TAG = "OverviewFragment"

private typealias BalancesAdapterT = ListAdapter<Balance, BalanceBriefViewHolder>

class OverviewFragment : Fragment(R.layout.fragment_overview) {

    private val viewModel: OverviewViewModel by viewModel()

    private var binding: FragmentOverviewBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentOverviewBinding.bind(view).also {
            this.binding = it
        }

        binding.sellEditText.addTextChangedListener(DecimalMaskTextWatcher())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest {
                        Timber.tag(TAG).d("UiState: %s", it)
                        binding.render(it)
                    }
                }
                launch {
                    viewModel.oneShotEvents().collectLatest { event ->
                        Timber.tag(TAG).d("single time event: %s", event)
                        when (event) {
                            OverviewViewModel.Event.ExchangeStarted ->
                                true

                            is OverviewViewModel.Event.ExchangeSuccess -> {
                                alertDialog(true, event.transaction)
                                false
                            }

                            is OverviewViewModel.Event.NotEnoughFunds -> {
                                alertDialog(false, event.transaction)
                                false
                            }
                        }.let {
                            binding.disableInputs(it)
                        }
                    }
                }
                launch {
                    binding.sellEditText
                        .editorActionEvents { actionId, event ->
                            Timber.tag(TAG).d("action: %d, key event: %s", actionId, event)
                            actionId == EditorInfo.IME_ACTION_DONE
                        }
                        .map(ParseTextInput)
                        .logEach(TAG, "parsed number: %s")
                        .collectLatest { parsedNumber ->
                            if (parsedNumber == null) {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.please_provide_valid_number,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                binding.root.hideSoftKeyboard()
                            }
                            viewModel.updateDischargeAmount(parsedNumber)
                        }
                }
                launch {
                    binding.sellDropdown.onItemSelectedFlow()
                        .mapNotNull(ConvertToCurrency)
                        .collectLatest {
                            viewModel.updateSourceCurrency(it)
                        }
                }
                launch {
                    binding.buyDropdown.onItemSelectedFlow()
                        .mapNotNull(ConvertToCurrency)
                        .collectLatest {
                            viewModel.updateTargetCurrency(it)
                        }
                }
                launch {
                    binding.submitExchangeButton.clicks()
                        .collectLatest {
                            viewModel.commitExchange()
                        }
                }
            }
        }
    }

    private fun alertDialog(success: Boolean, transaction: OverviewViewModel.Event.Transaction) {
        findNavController()
            .navigate(
                R.id.action_overview_fragment_to_transaction_alert_dialog,
                TransactionDialog.arguments(
                    success = success,
                    dischargeAmount = transaction.amount,
                    feeAmount = transaction.fee,
                    from = transaction.from,
                    converted = transaction.converted,
                    to = transaction.to,
                )
            )
    }

    override fun onDestroyView() {
        binding = null

        super.onDestroyView()
    }

    private fun FragmentOverviewBinding.render(state: OverviewViewModel.UiState) {
        if (state.loading) {
            displayLoading()
        } else {
            displayLoaded(state)
        }
    }

    private fun FragmentOverviewBinding.changeVisibility(
        showLoader: Boolean,
        showContent: Boolean,
        showUnrecoverableError: Boolean,
    ) {
        val contentVisibility = View.VISIBLE.takeIf { showContent } ?: View.GONE
        balancesTitle.visibility = contentVisibility
        balancesRecyclerView.visibility = contentVisibility
        exchangeTitle.visibility = contentVisibility
        sellCurrencyImage.visibility = contentVisibility
        sellLabel.visibility = contentVisibility
        sellEditText.visibility = contentVisibility
        sellDropdown.visibility = contentVisibility
        exchangeDivider.visibility = contentVisibility
        buyCurrencyImage.visibility = contentVisibility
        buyLabel.visibility = contentVisibility
        buyDisplayText.visibility = contentVisibility
        buyDropdown.visibility = contentVisibility
        submitExchangeButton.visibility = contentVisibility

        pageLoadingProgressBar.visibility = View.VISIBLE.takeIf { showLoader } ?: View.GONE

        unrecoverableErrorHappenedTitle.visibility =
            View.VISIBLE.takeIf { showUnrecoverableError } ?: View.GONE
    }

    private fun FragmentOverviewBinding.displayLoading() {
        changeVisibility(
            showLoader = true,
            showContent = false,
            showUnrecoverableError = false,
        )
    }

    private fun FragmentOverviewBinding.displayError() {
        changeVisibility(
            showLoader = false,
            showContent = false,
            showUnrecoverableError = true,
        )
    }

    private fun FragmentOverviewBinding.displayLoaded(state: OverviewViewModel.UiState) {
        setupBalancesAdapterIfRequired().run {
            submitList(state.balancesList)
        }

        sellDropdown.setupCurrenciesSpinnerIfRequired(
            state.availableSourceCurrencies,
            state.selectedSourceCurrency
        )
        buyDropdown.setupCurrenciesSpinnerIfRequired(
            state.availableTargetCurrencies,
            state.selectedTargetCurrency
        )

        changeVisibility(
            showLoader = false,
            showContent = true,
            showUnrecoverableError = false,
        )

        state.targetTransfer?.let {
            val numberInCents = it.toDouble() / 100
            buyDisplayText.setText(NumberFormat.getNumberInstance().format(numberInCents))
        }
    }

    private fun FragmentOverviewBinding.disableInputs(performingExchange: Boolean) {
        // disable button and inputs while exchange happens
        val inputsEnabled = !performingExchange
        sellEditText.isEnabled = inputsEnabled
        sellDropdown.isEnabled = inputsEnabled
        buyDropdown.isEnabled = inputsEnabled
        submitExchangeButton.isEnabled = inputsEnabled
    }

    private fun FragmentOverviewBinding.setupBalancesAdapterIfRequired(): BalancesAdapterT {
        @Suppress("UNCHECKED_CAST")
        return balancesRecyclerView.adapter as? BalancesAdapterT
            ?: BalancesAdapter(
                AsyncDifferConfig.Builder(BalanceDiffCallback)
                    .build()
            ).also {
                balancesRecyclerView.run {
                    setAdapter(it)
                    setHasFixedSize(true)
                }
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Spinner.setupCurrenciesSpinnerIfRequired(
        currencies: List<Currency>,
        selected: Currency
    ) {

        val oldAdapter = adapter as? ArrayAdapter<String>
        if (oldAdapter == null) {
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                currencies.map { it.identifier }
            ).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                Timber.tag(TAG)
                    .d("Create adapter for spinner: %s", resources.getResourceEntryName(id))
                adapter = it
            }
        } else {
            if (oldAdapter.contentShouldChange(currencies)) {
                Timber.tag(TAG)
                    .d("Change adapter for spinner: %s", resources.getResourceEntryName(id))
                oldAdapter.setNotifyOnChange(false)
                oldAdapter.clear()
                oldAdapter.addAll(currencies.map { it.identifier })
                oldAdapter.notifyDataSetChanged()
            }
        }

        val selectionPosition = currencies.indexOf(selected)
        setSelection(selectionPosition, false)
    }

    private fun ArrayAdapter<String>.contentShouldChange(
        newContent: List<Currency>
    ): Boolean {
        val oldItemsCount = count
        if (oldItemsCount != newContent.size) {
            return true
        }

        for (i in 0 until oldItemsCount) {
            val identifier = getItem(i)
            val currency = newContent[i]
            if (identifier != currency.identifier) {
                return true
            }
        }

        return false
    }
}

private object ConvertToCurrency : suspend (AdapterViewSelectionEvent) -> Currency? {

    override suspend fun invoke(selection: AdapterViewSelectionEvent) =
        (selection.selectedValue as? String)?.let {
            Currency(it)
        }
}
