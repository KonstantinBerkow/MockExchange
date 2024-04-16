package io.github.konstantinberkow.mockexchange.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class OverviewViewModel : ViewModel() {

    object Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            require(modelClass == OverviewViewModel::class.java)
            return OverviewViewModel() as T
        }
    }
}
