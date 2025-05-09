package com.example

import com.google.cloud.Timestamp
import com.google.cloud.firestore.annotation.DocumentId
import java.util.*

data class Club(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val members: MutableList<String> = mutableListOf(),
    val currentBook: String? = null,
    val createdAt: Timestamp = Timestamp.now()
)

data class CurrentBookRequest(val currentBook: String)
