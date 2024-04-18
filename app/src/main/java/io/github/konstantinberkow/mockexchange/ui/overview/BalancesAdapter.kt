package io.github.konstantinberkow.mockexchange.ui.overview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.ListAdapter
import io.github.konstantinberkow.mockexchange.databinding.BalanceItemBinding
import io.github.konstantinberkow.mockexchange.entity.Balance
import io.github.konstantinberkow.mockexchange.entity.Currency
import io.github.konstantinberkow.mockexchange.util.StableIdsProvider
import java.text.NumberFormat

class BalancesAdapter(
    config: AsyncDifferConfig<Balance>
) : ListAdapter<Balance, BalanceBriefViewHolder>(config) {

    private val stableIdsProvider = StableIdsProvider<Currency>()

    private val numberFormat = NumberFormat.getNumberInstance().also {
        it.minimumIntegerDigits = 1
        it.minimumFractionDigits = 2
        it.maximumFractionDigits = 2
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return stableIdsProvider.getIdFor(getItem(position).currency)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceBriefViewHolder {
        val binding = BalanceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BalanceBriefViewHolder(binding, numberFormat)
    }

    override fun onBindViewHolder(holder: BalanceBriefViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}