package io.github.konstantinberkow.mockexchange.entity

/**
 * Represents balance of specific account in given [Currency]
 *
 * @param currency currency of account
 * @param amount funds in cents
 */
data class Balance(
    val currency: Currency,
    val amount: UInt
)
