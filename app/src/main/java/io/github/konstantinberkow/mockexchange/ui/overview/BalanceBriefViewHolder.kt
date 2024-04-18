package io.github.konstantinberkow.mockexchange.ui.overview

import androidx.recyclerview.widget.RecyclerView
import io.github.konstantinberkow.mockexchange.databinding.BalanceItemBinding
import io.github.konstantinberkow.mockexchange.entity.Balance
import java.text.NumberFormat

class BalanceBriefViewHolder(
    private val binding: BalanceItemBinding,
    private val moneyFormatter: NumberFormat
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: Balance) {
        val text = buildString {
            append(moneyFormatter.format(item.amount.toDouble() / 100.0))
            append(" ${item.currency.identifier}")
        }
        binding.balanceTextView.text = text
    }
}
