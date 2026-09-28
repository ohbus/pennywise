package com.subhrodip.pennywise.accounts.profile.persistence

/** Compatibility facade combining profile command and query ports. */
interface ProfileStore : ProfileQueryStore, ProfileCommandStore
