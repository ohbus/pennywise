package com.subhrodip.pennywise.notifications.delivery.model

/** Result category used to choose retry or parking behavior. */
enum class DeliveryOutcome { RETRYABLE_FAILURE, PERMANENT_FAILURE, SUCCESS }
