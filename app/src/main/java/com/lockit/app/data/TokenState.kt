package com.lockit.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "token_state")
data class TokenState(
    @PrimaryKey val id: Int = 0, // singleton row
    val tokensRemaining: Int = 10,
    val weekStartEpochDay: Long,
    // If a token skip is currently active: the epoch millis when it expires, else null
    val activeSkipExpiresAtEpochMillis: Long? = null
)
