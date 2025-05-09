package com.example.models

/**
 * Represents a book entity with its basic metadata.
 *
 * @property title The title of the book.
 * @property authors A list of authors who wrote the book.
 * @property thumbnail An optional URL to the book's thumbnail image.
 *
 * This model is typically used for displaying book information
 * within the app or for interacting with external APIs and databases.
 */
data class Book(
    val title: String,
    val authors: List<String>,
    val thumbnail: String?
)
