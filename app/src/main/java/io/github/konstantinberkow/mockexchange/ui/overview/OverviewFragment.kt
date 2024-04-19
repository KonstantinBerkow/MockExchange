package io.github.konstantinberkow.mockexchange.ui.overview

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import io.github.konstantinberkow.mockexchange.ui.util.editorActionEvents
import io.github.konstantinberkow.mockexchange.ui.util.hideSoftKeyboard
import io.github.konstantinberkow.mockexchange.ui.util.onItemSelectedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

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

        binding.sellDropdown.onItemSelectedFlow()
            .map(ConvertToVMSelection(type = OverviewViewModel.Action.SelectedCurrency.Type.Sell))

        binding.buyDropdown.onItemSelectedFlow()
            .map(ConvertToVMSelection(type = OverviewViewModel.Action.SelectedCurrency.Type.Buy))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                val viewModelConsumer = viewModel::accept

                launch {
                    viewModel.state().collectLatest {
                        Timber.tag(TAG).d("UIState: %s", it)
                        when (it) {
                            OverviewViewModel.UiState.Loading -> binding.displayLoading()
                            is OverviewViewModel.UiState.Loaded -> binding.displayLoaded(it)
                            OverviewViewModel.UiState.LoadFailed -> binding.displayError()
                        }
                    }
                }
                launch {
                    viewModel.singleTimeEvents().collectLatest {
                        Timber.tag(TAG).d("single time event: %s", it)
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
                                binding.sellDropdown.performClick()
                                binding.root.hideSoftKeyboard()
                            }
                        }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.accept(OverviewViewModel.Action.Load)
    }

    override fun onDestroyView() {
        binding = null

        super.onDestroyView()
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

    private fun FragmentOverviewBinding.displayLoaded(state: OverviewViewModel.UiState.Loaded) {
        setupBalancesAdapterIfRequired().run {
            submitList(state.userFunds)
        }

        buyDropdown.setupBuySpinnerIfRequired(state.availableCurrencies)
        sellDropdown.setupBuySpinnerIfRequired(state.availableCurrencies)

        changeVisibility(
            showLoader = false,
            showContent = true,
            showUnrecoverableError = false,
        )

        // disable button and inputs while exchange happens
        val inputsEnabled = !state.performingExchange
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
    private fun Spinner.setupBuySpinnerIfRequired(
        currencies: List<Currency>
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
                val selectedCurrency = this.selectedItemPosition
                    .takeIf { it != AdapterView.INVALID_POSITION }
                    ?.let { oldAdapter.getItem(it) }
                Timber.tag(TAG).d("Selected currency: %s", selectedCurrency)
                val newList = currencies.map { it.identifier }
                val newSelectedPosition = selectedCurrency?.let {
                    newList.indexOf(it)
                }
                Timber.tag(TAG).d("Update position: %d", newSelectedPosition)

                Timber.tag(TAG)
                    .d("Change adapter for spinner: %s", resources.getResourceEntryName(id))
                oldAdapter.setNotifyOnChange(false)
                oldAdapter.clear()
                oldAdapter.addAll(newList)
                if (newSelectedPosition != null && newSelectedPosition >= 0) {
                    setSelection(newSelectedPosition)
                }
                oldAdapter.notifyDataSetChanged()
            }
        }
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

private class ConvertToVMSelection(
    private val type: OverviewViewModel.Action.SelectedCurrency.Type
) : suspend (AdapterViewSelectionEvent) -> OverviewViewModel.Action.SelectedCurrency {

    override suspend fun invoke(selection: AdapterViewSelectionEvent) =
        OverviewViewModel.Action.SelectedCurrency(
            type = type,
            currency = (selection.selectedValue as? String)?.let {
                Currency(it)
            }
        )
}
