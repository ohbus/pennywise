package com.subhrodip.squarewise.expensecore.expenses.persistence.store

/** Compatibility facade combining expense command and query ports. */
interface ExpenseStore : ExpenseCommandStore, ExpenseQueryStore
