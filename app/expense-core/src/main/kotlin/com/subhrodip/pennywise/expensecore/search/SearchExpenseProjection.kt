package com.subhrodip.pennywise.expensecore.search

/** Scalar read projection preventing search from loading an aggregate graph. */
interface SearchExpenseProjection {
    /** Expense identifier used for stable keyset pagination. */
    fun getExpenseId(): String
    /** Expense description. */
    fun getDescription(): String
    /** Stored category key. */
    fun getCategory(): String
    /** ISO currency code. */
    fun getCurrency(): String
    /** Integer minor-unit amount. */
    fun getAmountMinor(): Long
}
