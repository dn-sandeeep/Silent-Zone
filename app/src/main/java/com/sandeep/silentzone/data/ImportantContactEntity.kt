package com.sandeep.silentzone.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "important_contacts")
data class ImportantContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String
)
