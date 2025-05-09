package com.example.routes

import com.example.models.*
import com.example.service.FirebaseService
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

        // Public: List all clubs
        get {
            val db = FirebaseService.firestoreDb
            val snapshot = db.collection("clubs").get().get()
            val clubs = snapshot.documents.mapNotNull { it.toObject(Club::class.java) }
            call.respond(ApiResponse(success = true, data = clubs))
        }

        authenticate("auth-jwt") {

            post {
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
                call.respond(ApiResponse(success = true, data = club))
            }

            post("{id}/join") {
                val id = call.parameters["id"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (id.isNullOrBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID"))
                }

                val db = FirebaseService.firestoreDb
                val docRef = db.collection("clubs").document(id)
                val snapshot = docRef.get().get()

                val club = snapshot.toObject(Club::class.java)
                    ?: return@post call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to load club data"))

                if (club.members.contains(userEmail)) {
                    return@post call.respond(ApiResponse(success = true, data = mapOf("message" to "Already a member")))
                }

                docRef.update("members", FieldValue.arrayUnion(userEmail))
                call.respond(ApiResponse(success = true, data = mapOf("message" to "Joined club!", "clubId" to club.id)))
            }

            delete("{id}/leave") {
                val id = call.parameters["id"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (id.isNullOrBlank()) {
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID"))
                }

                val docRef = FirebaseService.firestoreDb.collection("clubs").document(id)
                val snapshot = docRef.get().get()
                val club = snapshot.toObject(Club::class.java)
                    ?: return@delete call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to parse club data"))

                if (!club.members.contains(userEmail)) {
                    return@delete call.respond(ApiResponse(success = true, data = mapOf("message" to "You are not a member of this club")))
                }

                val updatedMembers = club.members.toMutableList().apply { remove(userEmail) }
                docRef.update("members", updatedMembers)
                call.respond(ApiResponse(success = true, data = mapOf("message" to "Left the club", "clubId" to club.id)))
            }

            delete("{id}") {
                val id = call.parameters["id"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (id.isNullOrBlank()) {
                    return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID"))
                }

                val docRef = FirebaseService.firestoreDb.collection("clubs").document(id)
                val snapshot = docRef.get().get()
                val club = snapshot.toObject(Club::class.java)
                    ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Club not found"))

                if (club.createdBy != userEmail) {
                    return@delete call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(false, error = "Not authorized to delete this club"))
                }

                docRef.delete()
                call.respond(ApiResponse(success = true, data = mapOf("message" to "Club deleted!", "deletedClubId" to id)))
            }
        }

        patch("{id}/currentBook") {
            val id = call.parameters["id"]
            val userEmail = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()

            if (id.isNullOrBlank()) {
                return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID"))
            }

            val docRef = FirebaseService.firestoreDb.collection("clubs").document(id)
            val snapshot = try {
                docRef.get().get()
            } catch (e: Exception) {
                return@patch call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to fetch club: ${e.message}"))
            }

            val club = snapshot.toObject(Club::class.java)
                ?: return@patch call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Club not found"))

            val request = try {
                call.receive<CurrentBookRequest>()
            } catch (e: Exception) {
                return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Invalid request body: ${e.message}"))
            }

            if (request.currentBook.isBlank()) {
                return@patch call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Current book must not be blank"))
            }

            docRef.update("currentBook", request.currentBook)
            call.respond(ApiResponse(success = true, data = mapOf("message" to "Current book updated!", "clubId" to club.id, "currentBook" to request.currentBook)))
        }

        delete("{id}/currentBook") {
            val id = call.parameters["id"]
            val userEmail = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()

            if (id.isNullOrBlank()) {
                return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(false, error = "Missing club ID"))
            }

            val docRef = FirebaseService.firestoreDb.collection("clubs").document(id)
            val snapshot = try {
                docRef.get().get()
            } catch (e: Exception) {
                return@delete call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(false, error = "Failed to fetch club: ${e.message}"))
            }

            val club = snapshot.toObject(Club::class.java)
                ?: return@delete call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(false, error = "Club not found"))

            docRef.update("currentBook", FieldValue.delete())
            call.respond(ApiResponse(success = true, data = mapOf("message" to "Current book removed from club", "clubId" to id)))
        }
    }
}
