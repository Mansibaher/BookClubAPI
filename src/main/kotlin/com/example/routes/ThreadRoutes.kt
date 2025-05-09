package com.example.routes

import com.example.models.*
import com.example.service.FirebaseService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines all routes related to discussion threads and comments within a book club.
 *
 * Endpoint structure:
 * - GET /clubs/{clubId}/threads → Get all threads for a club.
 * - POST /clubs/{clubId}/threads → Create a new thread (auth required).
 * - GET /clubs/{clubId}/threads/{threadId} → Get a specific thread.
 * - DELETE /clubs/{clubId}/threads/{threadId} → Delete a thread (auth required, only creator).
 * - POST /clubs/{clubId}/threads/{threadId}/comments → Add comment to thread (auth required).
 * - DELETE /clubs/{clubId}/threads/{threadId}/comments/{commentId} → Delete comment (auth required, only creator).
 */
fun Route.threadRoutes() {
    route("/clubs/{clubId}/threads") {

        // Get all threads for a club
        get {
            val clubId = call.parameters["clubId"]
            if (clubId.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID"))
            }

            try {
                val db = FirebaseService.firestoreDb
                val clubDoc = db.collection("clubs").document(clubId).get().get()
                if (!clubDoc.exists()) {
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Club not found"))
                }

                val threadsSnapshot = db.collection("clubs").document(clubId).collection("threads").get().get()
                val threads = threadsSnapshot.documents.mapNotNull { it.toObject(Thread::class.java) }
                call.respond(ApiResponse(success = true, data = threads))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to fetch threads: ${e.message}"))
            }
        }

        // Create thread
        authenticate("auth-jwt") {
            post {
                val clubId = call.parameters["clubId"]
                if (clubId.isNullOrBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID"))
                }

                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val request = call.receive<ThreadRequest>()

                try {
                    val db = FirebaseService.firestoreDb
                    val clubDoc = db.collection("clubs").document(clubId).get().get()
                    if (!clubDoc.exists()) {
                        return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Club not found"))
                    }

                    val threadRef = db.collection("clubs").document(clubId).collection("threads").document()
                    val thread = Thread(threadRef.id, clubId, request.title, request.content, userEmail)

                    threadRef.set(thread).get()
                    call.respond(ApiResponse(success = true, data = thread))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to create thread: ${e.message}"))
                }
            }
        }

        // Get specific thread
        get("{threadId}") {
            val clubId = call.parameters["clubId"]
            val threadId = call.parameters["threadId"]

            if (clubId.isNullOrBlank() || threadId.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID or thread ID"))
            }

            try {
                val db = FirebaseService.firestoreDb
                val clubDoc = db.collection("clubs").document(clubId).get().get()
                if (!clubDoc.exists()) {
                    return@get call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Club not found"))
                }

                val threadDoc = db.collection("clubs").document(clubId).collection("threads").document(threadId).get().get()
                if (threadDoc.exists()) {
                    val thread = threadDoc.toObject(Thread::class.java)
                    call.respond(ApiResponse(success = true, data = thread))
                } else {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Thread not found"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to fetch thread: ${e.message}"))
            }
        }

        // Delete thread (creator only)
        authenticate("auth-jwt") {
            delete("{threadId}") {
                val clubId = call.parameters["clubId"]
                val threadId = call.parameters["threadId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (clubId.isNullOrBlank() || threadId.isNullOrBlank()) {
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID or thread ID"))
                }

                try {
                    val db = FirebaseService.firestoreDb
                    val clubDoc = db.collection("clubs").document(clubId).get().get()
                    if (!clubDoc.exists()) {
                        return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Club not found"))
                    }

                    val threadRef = db.collection("clubs").document(clubId).collection("threads").document(threadId)
                    val threadDoc = threadRef.get().get()
                    if (threadDoc.exists()) {
                        val thread = threadDoc.toObject(Thread::class.java)
                        if (thread?.createdBy == userEmail) {
                            val comments = threadRef.collection("comments").get().get()
                            for (comment in comments.documents) {
                                comment.reference.delete().get()
                            }
                            threadRef.delete().get()
                            call.respond(ApiResponse(success = true, data = mapOf("message" to "Thread deleted!", "deletedThreadId" to threadId)))
                        } else {
                            call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, error = "Not authorized to delete this thread"))
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Thread not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to delete thread: ${e.message}"))
                }
            }
        }

        // Add a comment
        authenticate("auth-jwt") {
            post("{threadId}/comments") {
                val clubId = call.parameters["clubId"]
                val threadId = call.parameters["threadId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val request = call.receive<CommentRequest>()

                if (clubId.isNullOrBlank() || threadId.isNullOrBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID or thread ID"))
                }

                try {
                    val db = FirebaseService.firestoreDb
                    val clubDoc = db.collection("clubs").document(clubId).get().get()
                    if (!clubDoc.exists()) {
                        return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Club not found"))
                    }

                    val threadRef = db.collection("clubs").document(clubId).collection("threads").document(threadId)
                    val threadDoc = threadRef.get().get()
                    if (!threadDoc.exists()) {
                        return@post call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Thread not found"))
                    }

                    val commentRef = threadRef.collection("comments").document()
                    val comment = Comment(commentRef.id, request.content, userEmail)
                    commentRef.set(comment).get()

                    call.respond(ApiResponse(success = true, data = comment))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to add comment: ${e.message}"))
                }
            }
        }

        // Delete comment
        authenticate("auth-jwt") {
            delete("{threadId}/comments/{commentId}") {
                val clubId = call.parameters["clubId"]
                val threadId = call.parameters["threadId"]
                val commentId = call.parameters["commentId"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (clubId.isNullOrBlank() || threadId.isNullOrBlank() || commentId.isNullOrBlank()) {
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID, thread ID, or comment ID"))
                }

                try {
                    val db = FirebaseService.firestoreDb
                    val clubDoc = db.collection("clubs").document(clubId).get().get()
                    if (!clubDoc.exists()) {
                        return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Club not found"))
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
                            commentRef.delete().get()
                            call.respond(ApiResponse(success = true, data = mapOf("message" to "Comment deleted!", "deletedCommentId" to commentId)))
                        } else {
                            call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, error = "You are not authorized to delete this comment"))
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Comment not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to delete comment: ${e.message}"))
                }
            }
        }
    }
}
