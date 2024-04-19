package io.github.konstantinberkow.mockexchange.ui.overview

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import io.github.konstantinberkow.mockexchange.R
import io.github.konstantinberkow.mockexchange.entity.Currency
import java.text.NumberFormat

private const val SUCCESS_KEY = "success"
private const val TARGET_KEY = "target"
private const val SOURCE_KEY = "source"
private const val DISCHARGE_AMOUNT_KEY = "discharge_amount"
private const val TARGET_AMOUNT_KEY = "target_amount"
private const val FEE_KEY = "fee_amount"

class TransactionDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val msg = arguments?.let { args ->

            val success = args.getBoolean(SUCCESS_KEY, false)
            val dischargeAmount = args.readFunds(DISCHARGE_AMOUNT_KEY)
            val feeAmount = args.readFunds(FEE_KEY)
            val sourceCurrency = args.getString(SOURCE_KEY, Currency.EUR.identifier)
            val targetAmount = args.readFunds(TARGET_AMOUNT_KEY)
            val targetCurrency = args.getString(TARGET_KEY, Currency.USD.identifier)

            val template = if (success) {
                R.string.success_exchange_template
            } else {
                R.string.failure_exchange_template
            }

            val format = NumberFormat.getNumberInstance()

            getString(
                template,
                format.format(dischargeAmount),
                sourceCurrency,
                format.format(targetAmount),
                targetCurrency,
                format.format(feeAmount)
            )
        } ?: "Args not specified!"

        return AlertDialog.Builder(requireContext())
            .setMessage(msg)
            .create()
    }

    companion object {
        fun arguments(
            success: Boolean,
            dischargeAmount: UInt,
            feeAmount: UInt,
            from: Currency,
            converted: UInt,
            to: Currency
        ): Bundle = bundleOf(
            SUCCESS_KEY to success,
            DISCHARGE_AMOUNT_KEY to dischargeAmount.toLong(),
            FEE_KEY to feeAmount.toLong(),
            SOURCE_KEY to from.identifier,
            TARGET_AMOUNT_KEY to converted.toLong(),
            TARGET_KEY to to.identifier
        )
    }
}

private fun Bundle.readFunds(key: String): Double {
    return getLong(key, 0L).toDouble() / 100
}
