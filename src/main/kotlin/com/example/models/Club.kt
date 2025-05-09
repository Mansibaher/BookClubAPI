package com.example.models

import com.google.cloud.Timestamp

/**
 * Represents a book club with its metadata and current state.
 *
 * @property id Unique identifier for the club (e.g., Firestore document ID).
 * @property name Name of the book club.
 * @property description Brief description of the club’s purpose or theme.
 * @property createdBy The user ID or email of the club's creator.
 * @property members A mutable list of user IDs representing the club's members.
 * @property currentBook Optional ID or title of the book currently being read.
 * @property createdAt Timestamp of when the club was created (defaults to current time).
 *
 * This model is used to store and retrieve book club information from Firestore.
 */
data class Club(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val members: MutableList<String> = mutableListOf(),
    val currentBook: String? = null,
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * Data class for updating the currently selected book in a club.
 *
 * @property currentBook The ID or title of the new current book.
 *
 * Typically used in PATCH or PUT API requests to change a club's current book.
 */
data class CurrentBookRequest(val currentBook: String)
