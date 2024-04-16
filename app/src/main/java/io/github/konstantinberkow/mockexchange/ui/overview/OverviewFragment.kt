package io.github.konstantinberkow.mockexchange.ui.overview

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.github.konstantinberkow.mockexchange.R

class OverviewFragment : Fragment(R.layout.fragment_overview) {

    private val viewModel by activityViewModels<OverviewViewModel> {
        OverviewViewModel.Factory
    }
}
