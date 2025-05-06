package com.example

import com.example.models.Thread
import com.example.models.ThreadRequest
import com.example.models.Comment
import com.example.models.CommentRequest
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// In-memory storage for threads
val threads = mutableListOf<Thread>()

fun Route.threadRoutes() {
    route("/clubs/{clubId}/threads") {
        // Get all threads for a club
        get {
            val clubId = call.parameters["clubId"]
            val clubThreads = threads.filter { it.clubId == clubId }
            call.respond(clubThreads)
        }

        // Create a new thread (authenticated)
        authenticate("auth-jwt") {
            post {
                val clubId = call.parameters["clubId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val request = call.receive<ThreadRequest>()

                val thread = Thread(
                    clubId = clubId!!,
                    title = request.title,
                    content = request.content,
                    createdBy = userEmail
                )

                threads.add(thread)
                call.respond(mapOf("message" to "Thread created!", "thread" to thread))
            }
        }

        // Get a specific thread
        get("{threadId}") {
            val threadId = call.parameters["threadId"]
            val thread = threads.find { it.id == threadId }
            
            if (thread != null) {
                call.respond(thread)
            } else {
                call.respond(mapOf("error" to "Thread not found"))
            }
        }

        // Delete a thread (authenticated, only creator can delete)
        authenticate("auth-jwt") {
            delete("{threadId}") {
                val threadId = call.parameters["threadId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val thread = threads.find { it.id == threadId }

                if (thread != null) {
                    if (thread.createdBy == userEmail) {
                        threads.remove(thread)
                        call.respond(mapOf("message" to "Thread deleted!", "deletedThreadId" to thread.id))
                    } else {
                        call.respond(mapOf("error" to "You are not authorized to delete this thread"))
                    }
                } else {
                    call.respond(mapOf("error" to "Thread not found"))
                }
            }
        }

        // Add a comment to a thread (authenticated)
        authenticate("auth-jwt") {
            post("{threadId}/comments") {
                val threadId = call.parameters["threadId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val request = call.receive<CommentRequest>()
                val thread = threads.find { it.id == threadId }

                if (thread != null) {
                    val comment = Comment(
                        content = request.content,
                        createdBy = userEmail
                    )
                    thread.comments.add(comment)
                    call.respond(mapOf("message" to "Comment added!", "comment" to comment))
                } else {
                    call.respond(mapOf("error" to "Thread not found"))
                }
            }
        }

        // Delete a comment (authenticated, only comment creator can delete)
        authenticate("auth-jwt") {
            delete("{threadId}/comments/{commentId}") {
                val threadId = call.parameters["threadId"]
                val commentId = call.parameters["commentId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val thread = threads.find { it.id == threadId }

                if (thread != null) {
                    val comment = thread.comments.find { it.id == commentId }
                    if (comment != null) {
                        if (comment.createdBy == userEmail) {
                            thread.comments.remove(comment)
                            call.respond(mapOf("message" to "Comment deleted!", "deletedCommentId" to comment.id))
                        } else {
                            call.respond(mapOf("error" to "You are not authorized to delete this comment"))
                        }
                    } else {
                        call.respond(mapOf("error" to "Comment not found"))
                    }
                } else {
                    call.respond(mapOf("error" to "Thread not found"))
                }
            }
        }
    }
} 