package com.example.models

import com.google.cloud.Timestamp

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
