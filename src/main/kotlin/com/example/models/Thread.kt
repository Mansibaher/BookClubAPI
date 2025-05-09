package com.example.models

import com.google.cloud.Timestamp
import com.google.cloud.firestore.annotation.DocumentId
import java.util.*

/**
 * Represents a discussion thread within a book club.
 *
 * @property id Unique identifier for the thread (Firestore document ID).
 * @property clubId Identifier of the club where this thread belongs.
 * @property title Title of the discussion thread.
 * @property content Main content or body of the thread.
 * @property createdBy User ID or email of the thread creator.
 * @property createdAt Timestamp indicating when the thread was created.
 */
data class Thread(
    @DocumentId
    val id: String = "",
    val clubId: String = "",
    val title: String = "",
    val content: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * Represents a comment made on a discussion thread.
 *
 * @property id Unique identifier for the comment (Firestore document ID).
 * @property content Text content of the comment.
 * @property createdBy User ID or email of the comment author.
 * @property createdAt Timestamp indicating when the comment was posted.
 */
data class Comment(
    @DocumentId
    val id: String = "",
    val content: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * Payload for creating a new discussion thread within a club.
 *
 * @property title The title of the new thread.
 * @property content The initial content or description of the thread.
 */
data class ThreadRequest(
    val title: String,
    val content: String
)

/**
 * Payload for posting a new comment on a thread.
 *
 * @property content The text content of the comment.
 */
data class CommentRequest(
    val content: String
) 