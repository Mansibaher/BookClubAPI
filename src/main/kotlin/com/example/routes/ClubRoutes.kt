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

fun Route.clubRoutes() {
    route("/clubs") {

        // Lambda assigned to variable ✅
        val formatMessage: (String) -> Map<String, String> = { msg -> mapOf("message" to msg) }

        // GET all clubs ✅
        get {
            respondSafely(call) {
                val snapshot = FirebaseService.firestoreDb.collection("clubs").get().get()
                snapshot.documents.mapNotNull { it.toObject(Club::class.java) }
            }
        }

        authenticate("auth-jwt") {

            // POST /clubs ✅
            post {
                respondSafely(call) {
                    val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                    val request = call.receive<ClubRequest>()
                    val clubId = UUID.randomUUID().toString()

                    val club = Club(
                        id = clubId,
                        name = request.name,
                        description = request.description,
                        createdBy = userEmail,
                        members = mutableListOf(userEmail),
                        currentBook = request.currentBook
                    )

                    FirebaseService.firestoreDb.collection("clubs").document(clubId).set(club)
                    club
                }
            }

            // POST /clubs/{id}/join ✅
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

            // DELETE /clubs/{id}/leave ✅
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

            // DELETE /clubs/{id} ✅
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

        // PATCH /clubs/{id}/currentBook ✅
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

        // DELETE /clubs/{id}/currentBook ✅
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
