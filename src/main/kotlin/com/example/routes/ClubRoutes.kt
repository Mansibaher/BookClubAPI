package com.example.routes

import com.example.models.*
import com.example.service.FirebaseService
import com.example.utils.respondSafely
import com.google.cloud.firestore.FieldValue
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Defines all routes related to book club management.
 *
 * Routes:
 * - GET /clubs: List all book clubs.
 * - POST /clubs: Create a new club (requires auth).
 * - POST /clubs/{id}/join: Join an existing club (requires auth).
 * - DELETE /clubs/{id}/leave: Leave a club (requires auth).
 * - DELETE /clubs/{id}: Delete a club if user is creator (requires auth).
 * - PATCH /clubs/{id}/currentBook: Update the club's current book.
 * - DELETE /clubs/{id}/currentBook: Remove the club's current book.
 */
fun Route.clubRoutes() {
    route("/clubs") {

        // Utility to return consistent message responses using Lambda
        val formatMessage: (String) -> Map<String, String> = { msg -> mapOf("message" to msg) }

        // GET /clubs — fetch all clubs (public)
        get {
            respondSafely(call) {
                val snapshot = FirebaseService.firestoreDb.collection("clubs").get().get()
                snapshot.documents.mapNotNull { it.toObject(Club::class.java) }
            }
        }

        authenticate("auth-jwt") {

            // POST /clubs — create a new club
            post {
                respondSafely(call) {
                    val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                    val request = call.receive<ClubRequest>()
                    val clubId = UUID.randomUUID().toString()

                    // Safely handle nullable members list and ensure current user is added
                    val members = request.members.orEmpty().toMutableSet().apply {
                        add(userEmail)
                    }.toMutableList()

                    val club = Club(
                        id = clubId,
                        name = request.name,
                        description = request.description,
                        createdBy = userEmail,
                        members = members,
                        currentBook = request.currentBook
                    )

                    FirebaseService.firestoreDb.collection("clubs").document(clubId).set(club)
                    club
                }
            }



            // POST /clubs/{id}/join — add user to club members
            post("{id}/join") {
                respondSafely(call) {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing club ID")
                    val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                    val docRef = FirebaseService.firestoreDb.collection("clubs").document(id)
                    val snapshot = docRef.get().get()
                    val club = snapshot.toObject(Club::class.java) ?: throw Exception("Club not found")

                    if (club.members.contains(userEmail)) {
                        return@respondSafely formatMessage("Already a member")
                    }

                    docRef.update("members", FieldValue.arrayUnion(userEmail))
                    mapOf("message" to "Joined club!", "clubId" to club.id)
                }
            }

            // DELETE /clubs/{id}/leave — remove user from club members
            delete("{id}/leave") {
                respondSafely(call) {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing club ID")
                    val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                    val docRef = FirebaseService.firestoreDb.collection("clubs").document(id)
                    val snapshot = docRef.get().get()
                    val club = snapshot.toObject(Club::class.java) ?: throw Exception("Club not found")

                    if (!club.members.contains(userEmail)) {
                        return@respondSafely formatMessage("You are not a member of this club")
                    }

                    // Lambda with receiver ✅
                    val updatedMembers = club.members.toMutableList().apply {
                        remove(userEmail)
                    }

                    docRef.update("members", updatedMembers)
                    formatMessage("Left the club").plus("clubId" to club.id)
                }
            }

            // DELETE /clubs/{id} — delete club (only by creator)
            delete("{id}") {
                respondSafely(call) {
                    val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing club ID")
                    val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                    val docRef = FirebaseService.firestoreDb.collection("clubs").document(id)
                    val snapshot = docRef.get().get()
                    val club = snapshot.toObject(Club::class.java) ?: throw Exception("Club not found")

                    if (club.createdBy != userEmail) {
                        throw Exception("Not authorized to delete this club")
                    }

                    docRef.delete()
                    formatMessage("Club deleted!").plus("deletedClubId" to id)
                }
            }
        }

        // PATCH /clubs/{id}/currentBook — update current book (publicly callable)
        patch("{id}/currentBook") {
            respondSafely(call) {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing club ID")
                val userEmail = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()

                val docRef = FirebaseService.firestoreDb.collection("clubs").document(id)
                val snapshot = docRef.get().get()
                val club = snapshot.toObject(Club::class.java) ?: throw Exception("Club not found")

                val request = call.receive<CurrentBookRequest>()
                if (request.currentBook.isBlank()) throw IllegalArgumentException("Current book must not be blank")

                docRef.update("currentBook", request.currentBook)
                mapOf(
                    "message" to "Current book updated!",
                    "clubId" to club.id,
                    "currentBook" to request.currentBook
                )
            }
        }

        // DELETE /clubs/{id}/currentBook — remove current book
        delete("{id}/currentBook") {
            respondSafely(call) {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing club ID")
                val userEmail = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()

                val docRef = FirebaseService.firestoreDb.collection("clubs").document(id)
                val snapshot = docRef.get().get()
                val club = snapshot.toObject(Club::class.java) ?: throw Exception("Club not found")

                docRef.update("currentBook", FieldValue.delete())
                formatMessage("Current book removed from club").plus("clubId" to id)
            }
        }
    }
}
