package io.github.konstantinberkow.mockexchange.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class ExchangeViewModel : ViewModel() {

    object Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass == ExchangeViewModel::class.java)
            return ExchangeViewModel() as T
        }
    }
}
