package com.example.models

import java.util.*

data class Thread(
    val id: String = UUID.randomUUID().toString(),
    val clubId: String,
    val title: String,
    val content: String,
    val createdBy: String,
    val createdAt: Date = Date(),
    val comments: MutableList<Comment> = mutableListOf()
)

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val createdBy: String,
    val createdAt: Date = Date()
)

data class ThreadRequest(
    val title: String,
    val content: String
)

data class CommentRequest(
    val content: String
) 