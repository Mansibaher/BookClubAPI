package com.example

import com.example.models.Thread
import com.example.models.ThreadRequest
import com.example.models.Comment
import com.example.models.CommentRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.threadRoutes() {
    route("/clubs/{clubId}/threads") {
        // Get all threads for a club
        get {
            val clubId = call.parameters["clubId"]
            if (clubId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing club ID"))
                return@get
            }

            try {
                val db = FirebaseService.firestoreDb
                // First verify club exists
                val clubDoc = db.collection("clubs").document(clubId).get().get()
                if (!clubDoc.exists()) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Club not found"))
                    return@get
                }

                val threadsSnapshot = db.collection("clubs")
                    .document(clubId)
                    .collection("threads")
                    .get()
                    .get()

                val threads = threadsSnapshot.documents.mapNotNull { it.toObject(Thread::class.java) }
                call.respond(threads)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch threads: ${e.message}"))
            }
        }

        // Create a new thread (authenticated)
        authenticate("auth-jwt") {
            post {
                val clubId = call.parameters["clubId"]
                if (clubId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing club ID"))
                    return@post
                }

                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val request = call.receive<ThreadRequest>()

                try {
                    val db = FirebaseService.firestoreDb
                    // First verify club exists
                    val clubDoc = db.collection("clubs").document(clubId).get().get()
                    if (!clubDoc.exists()) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Club not found"))
                        return@post
                    }

                    val threadRef = db.collection("clubs")
                        .document(clubId)
                        .collection("threads")
                        .document()

                    val thread = Thread(
                        id = threadRef.id,
                        clubId = clubId,
                        title = request.title,
                        content = request.content,
                        createdBy = userEmail
                    )

                    threadRef.set(thread).get() // Wait for the write to complete
                    call.respond(mapOf("message" to "Thread created!", "thread" to thread))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create thread: ${e.message}"))
                }
            }
        }

        // Get a specific thread
        get("{threadId}") {
            val clubId = call.parameters["clubId"]
            val threadId = call.parameters["threadId"]
            
            if (clubId.isNullOrBlank() || threadId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing club ID or thread ID"))
                return@get
            }

            try {
                val db = FirebaseService.firestoreDb
                // First verify club exists
                val clubDoc = db.collection("clubs").document(clubId).get().get()
                if (!clubDoc.exists()) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Club not found"))
                    return@get
                }

                val threadDoc = db.collection("clubs")
                    .document(clubId)
                    .collection("threads")
                    .document(threadId)
                    .get()
                    .get()

                if (threadDoc.exists()) {
                    val thread = threadDoc.toObject(Thread::class.java)
                    call.respond(thread!!)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Thread not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to fetch thread: ${e.message}"))
            }
        }

        // Delete a thread (authenticated, only creator can delete)
        authenticate("auth-jwt") {
            delete("{threadId}") {
                val clubId = call.parameters["clubId"]
                val threadId = call.parameters["threadId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (clubId.isNullOrBlank() || threadId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing club ID or thread ID"))
                    return@delete
                }

                try {
                    val db = FirebaseService.firestoreDb
                    // First verify club exists
                    val clubDoc = db.collection("clubs").document(clubId).get().get()
                    if (!clubDoc.exists()) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Club not found"))
                        return@delete
                    }

                    val threadRef = db.collection("clubs")
                        .document(clubId)
                        .collection("threads")
                        .document(threadId)

                    val threadDoc = threadRef.get().get()
                    if (threadDoc.exists()) {
                        val thread = threadDoc.toObject(Thread::class.java)
                        if (thread?.createdBy == userEmail) {
                            // Delete all comments first
                            val commentsSnapshot = threadRef.collection("comments").get().get()
                            for (comment in commentsSnapshot.documents) {
                                comment.reference.delete().get() // Wait for each delete to complete
                            }
                            // Delete the thread
                            threadRef.delete().get() // Wait for delete to complete
                            call.respond(mapOf("message" to "Thread deleted!", "deletedThreadId" to threadId))
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You are not authorized to delete this thread"))
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Thread not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to delete thread: ${e.message}"))
                }
            }
        }

        // Add a comment to a thread (authenticated)
        authenticate("auth-jwt") {
            post("{threadId}/comments") {
                val clubId = call.parameters["clubId"]
                val threadId = call.parameters["threadId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val request = call.receive<CommentRequest>()

                if (clubId.isNullOrBlank() || threadId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing club ID or thread ID"))
                    return@post
                }

                try {
                    val db = FirebaseService.firestoreDb
                    // First verify club exists
                    val clubDoc = db.collection("clubs").document(clubId).get().get()
                    if (!clubDoc.exists()) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Club not found"))
                        return@post
                    }

                    val threadRef = db.collection("clubs")
                        .document(clubId)
                        .collection("threads")
                        .document(threadId)

                    // Verify thread exists
                    val threadDoc = threadRef.get().get()
                    if (!threadDoc.exists()) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Thread not found"))
                        return@post
                    }

                    val commentRef = threadRef.collection("comments").document()
                    val comment = Comment(
                        id = commentRef.id,
                        content = request.content,
                        createdBy = userEmail
                    )

                    commentRef.set(comment).get() // Wait for the write to complete
                    call.respond(mapOf("message" to "Comment added!", "comment" to comment))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to add comment: ${e.message}"))
                }
            }
        }

        // Delete a comment (authenticated, only comment creator can delete)
        authenticate("auth-jwt") {
            delete("{threadId}/comments/{commentId}") {
                val clubId = call.parameters["clubId"]
                val threadId = call.parameters["threadId"]
                val commentId = call.parameters["commentId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (clubId.isNullOrBlank() || threadId.isNullOrBlank() || commentId.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing club ID, thread ID, or comment ID"))
                    return@delete
                }

                try {
                    val db = FirebaseService.firestoreDb
                    // First verify club exists
                    val clubDoc = db.collection("clubs").document(clubId).get().get()
                    if (!clubDoc.exists()) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Club not found"))
                        return@delete
                    }

                    val commentRef = db.collection("clubs")
                        .document(clubId)
                        .collection("threads")
                        .document(threadId)
                        .collection("comments")
                        .document(commentId)

                    val commentDoc = commentRef.get().get()
                    if (commentDoc.exists()) {
                        val comment = commentDoc.toObject(Comment::class.java)
                        if (comment?.createdBy == userEmail) {
                            commentRef.delete().get() // Wait for delete to complete
                            call.respond(mapOf("message" to "Comment deleted!", "deletedCommentId" to commentId))
                        } else {
                            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You are not authorized to delete this comment"))
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Comment not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to delete comment: ${e.message}"))
                }
            }
        }
    }
} 