package io.github.konstantinberkow.mockexchange.ui.overview

import androidx.recyclerview.widget.DiffUtil
import io.github.konstantinberkow.mockexchange.entity.Balance

object BalanceDiffCallback : DiffUtil.ItemCallback<Balance>() {

    override fun areItemsTheSame(oldItem: Balance, newItem: Balance): Boolean {
        return oldItem.currency == newItem.currency
    }

    override fun areContentsTheSame(oldItem: Balance, newItem: Balance): Boolean {
        return oldItem.amount == newItem.amount
    }
}
