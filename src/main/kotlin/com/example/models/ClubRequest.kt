package com.example.models

/**
 * Represents the request payload for creating a new book club.
 *
 * @property name The name of the book club to be created.
 * @property description A short description of the club's purpose or theme.
 * @property currentBook The ID or title of the book to start with as the club's current read.
 *
 * This model is typically used in POST requests when a user creates a new book club.
 */
data class ClubRequest(
    val name: String,
    val description: String,
    val currentBook: String,
    val members: List<String>? = null

)
