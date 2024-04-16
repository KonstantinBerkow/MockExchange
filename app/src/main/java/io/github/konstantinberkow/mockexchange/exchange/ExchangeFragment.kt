package io.github.konstantinberkow.mockexchange.exchange

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.github.konstantinberkow.mockexchange.R
import io.github.konstantinberkow.mockexchange.databinding.FragmentExchangeBinding

class ExchangeFragment : Fragment(R.layout.fragment_exchange) {

    private val viewModel by activityViewModels<ExchangeViewModel> {
        ExchangeViewModel.Factory
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentExchangeBinding.bind(view)
    }
}
