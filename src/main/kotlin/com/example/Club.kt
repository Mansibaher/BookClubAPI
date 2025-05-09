package com.example

data class Club(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val members: MutableList<String> = mutableListOf(),
    val currentBook: String? = null
)

data class CurrentBookRequest(val currentBook: String)
