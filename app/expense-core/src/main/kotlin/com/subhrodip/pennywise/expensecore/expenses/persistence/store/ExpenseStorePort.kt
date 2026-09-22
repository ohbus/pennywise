package com.subhrodip.pennywise.expensecore.expenses.persistence.store

/** Compatibility facade combining expense command and query ports. */
interface ExpenseStore : ExpenseCommandStore, ExpenseQueryStore
