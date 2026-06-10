package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val name: String,
    val registrationTimestamp: Long = System.currentTimeMillis(),
    val welcomeEmailSent: Boolean = false
)
