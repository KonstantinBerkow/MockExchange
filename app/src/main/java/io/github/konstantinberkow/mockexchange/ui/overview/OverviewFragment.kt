package io.github.konstantinberkow.mockexchange.ui.overview

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import io.github.konstantinberkow.mockexchange.R
import io.github.konstantinberkow.mockexchange.databinding.FragmentOverviewBinding
import io.github.konstantinberkow.mockexchange.entity.Balance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

private const val TAG = "OverviewFragment"

class OverviewFragment : Fragment(R.layout.fragment_overview) {

    private val viewModel: OverviewViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentOverviewBinding.bind(view)

        val timber = Timber.tag(TAG)

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
        buyEditText.visibility = contentVisibility
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

    private var balancesAdapter: ListAdapter<Balance, BalanceBriefViewHolder>? = null

    private fun FragmentOverviewBinding.displayLoaded(state: OverviewViewModel.UiState.Loaded) {
        // lazily setup adapter, adapter outlives view
        val oldAdapter: ListAdapter<Balance, BalanceBriefViewHolder>? = balancesAdapter
        val adapter: ListAdapter<Balance, BalanceBriefViewHolder>
        if (oldAdapter == null) {
            adapter = BalancesAdapter(
                AsyncDifferConfig.Builder(BalanceDiffCallback)
                    .build()
            ).also {
                balancesAdapter = it
            }
        } else {
            adapter = oldAdapter
        }
        if (balancesRecyclerView.adapter == null) {
            balancesRecyclerView.run {
                setAdapter(adapter)
                setHasFixedSize(true)
            }
        }

        adapter.submitList(state.userFunds)

        changeVisibility(
            showLoader = false,
            showContent = true,
            showUnrecoverableError = false,
        )

        // disable button and inputs while exchange happens
        val inputsEnabled = !state.performingExchange
        buyEditText.isEnabled = inputsEnabled
        buyDropdown.isEnabled = inputsEnabled
        sellEditText.isEnabled = inputsEnabled
        sellDropdown.isEnabled = inputsEnabled
        submitExchangeButton.isEnabled = inputsEnabled
    }
}
