package com.example.models

import com.google.cloud.Timestamp
import com.google.cloud.firestore.annotation.DocumentId
import java.util.*

data class Thread(
    @DocumentId
    val id: String = "",
    val clubId: String = "",
    val title: String = "",
    val content: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class Comment(
    @DocumentId
    val id: String = "",
    val content: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class ThreadRequest(
    val title: String,
    val content: String
)

data class CommentRequest(
    val content: String
) 