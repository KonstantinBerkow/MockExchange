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

    private fun FragmentOverviewBinding.displayLoading() {
        balancesTitle.visibility = View.GONE
        balancesRecyclerView.visibility = View.GONE
        exchangeTitle.visibility = View.GONE
        submitExchangeButton.visibility = View.GONE
        pageLoadingProgressBar.visibility = View.VISIBLE
        unrecoverableErrorHappenedTitle.visibility = View.GONE
    }

    private fun FragmentOverviewBinding.displayError() {
        balancesTitle.visibility = View.GONE
        balancesRecyclerView.visibility = View.GONE
        exchangeTitle.visibility = View.GONE
        submitExchangeButton.visibility = View.GONE
        pageLoadingProgressBar.visibility = View.GONE
        unrecoverableErrorHappenedTitle.visibility = View.VISIBLE
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

        balancesTitle.visibility = View.VISIBLE
        balancesRecyclerView.visibility = View.VISIBLE
        exchangeTitle.visibility = View.VISIBLE
        submitExchangeButton.visibility = View.VISIBLE
        pageLoadingProgressBar.visibility = View.GONE
        unrecoverableErrorHappenedTitle.visibility = View.GONE

        // disable button while exchange happens
        submitExchangeButton.isEnabled = !state.performingExchange
    }
}
