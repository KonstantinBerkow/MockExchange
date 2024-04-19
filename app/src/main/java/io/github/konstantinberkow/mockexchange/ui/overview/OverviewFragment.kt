package io.github.konstantinberkow.mockexchange.ui.overview

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

private const val TAG = "OverviewFragment"

private typealias BalancesAdapterT = ListAdapter<Balance, BalanceBriefViewHolder>

class OverviewFragment : Fragment(R.layout.fragment_overview) {

    private val viewModel: OverviewViewModel by viewModel()

    private val timber = Timber.tag(TAG)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentOverviewBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                launch {
                    viewModel.state().collectLatest {
                        timber.d("UIState: %s", it)
                        when (it) {
                            OverviewViewModel.UiState.Loading -> binding.displayLoading()
                            is OverviewViewModel.UiState.Loaded -> binding.displayLoaded(it)
                            OverviewViewModel.UiState.LoadFailed -> binding.displayError()
                        }
                    }
                }
                launch {
                    viewModel.singleTimeEvents().collectLatest {
                        timber.d("single time event: %s", it)
                    }
                }
            }
        }

        viewModel.accept(OverviewViewModel.Action.Load)
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
                timber.d("Create adapter for spinner: %s", resources.getResourceEntryName(id))
                adapter = it
            }
        } else {
            if (oldAdapter.contentShouldChange(currencies)) {
                timber.d("Change adapter for spinner: %s", resources.getResourceEntryName(id))
                oldAdapter.setNotifyOnChange(false)
                oldAdapter.clear()
                oldAdapter.addAll(currencies.map { it.identifier })
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
