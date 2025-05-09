package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

val clubs = mutableListOf<Club>()

fun Route.clubRoutes() {
    route("/clubs") {

        // Public: List all clubs from Firestore
        get {
            val db = FirebaseService.firestoreDb
            val snapshot = db.collection("clubs").get().get()  // blocking get()
            val clubs = snapshot.documents.mapNotNull { it.toObject(Club::class.java) }
            call.respond(clubs)
        }

        // Authenticated: Create + Join + Leave + Delete
        authenticate("auth-jwt") {

            // ✅ Create a club
            // ✅ Create a club and store in Firestore
            post {
                val principal = call.principal<JWTPrincipal>()
                val userEmail = principal!!.payload.getClaim("email").asString()

                val request = call.receive<ClubRequest>()
                val clubId = UUID.randomUUID().toString()

                val club = Club(
                    id = clubId,
                    name = request.name,
                    description = request.description,
                    createdBy = userEmail,
                    members = mutableListOf(userEmail)
                )

                val db = FirebaseService.firestoreDb
                db.collection("clubs").document(clubId).set(club)

                call.respond(mapOf("message" to "Club created!", "club" to club))
            }

            // ✅ Join a club
            post("{id}/join") {
                val id = call.parameters["id"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (id == null) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing club ID"))
                }

                val db = FirebaseService.firestoreDb
                val docRef = db.collection("clubs").document(id)

                val snapshot = docRef.get().get()

                if (!snapshot.exists()) {
                    return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Club not found"))
                }

                val club = snapshot.toObject(Club::class.java)

                if (club == null) {
                    return@post call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to load club data"))
                }

                if (club.members.contains(userEmail)) {
                    return@post call.respond(mapOf("message" to "Already a member"))
                }

                // ✅ Use Firestore's atomic arrayUnion to avoid race conditions
                docRef.update("members", com.google.cloud.firestore.FieldValue.arrayUnion(userEmail))

                call.respond(mapOf("message" to "Joined club!", "clubId" to club.id))
            }


            // ✅ Leave a club
            delete("{id}/leave") {
                val id = call.parameters["id"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (id == null) {
                    return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing club ID"))
                }

                val db = FirebaseService.firestoreDb
                val docRef = db.collection("clubs").document(id)

                val snapshot = docRef.get().get()

                if (!snapshot.exists()) {
                    return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Club not found"))
                }

                val club = snapshot.toObject(Club::class.java)

                if (club == null) {
                    return@delete call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to parse club data"))
                }

                if (!club.members.contains(userEmail)) {
                    return@delete call.respond(mapOf("message" to "You are not a member of this club"))
                }

                // Remove user from members list and update Firestore
                val updatedMembers = club.members.toMutableList().apply { remove(userEmail) }

                docRef.update("members", updatedMembers)
                call.respond(mapOf("message" to "Left the club", "clubId" to club.id))
            }


            // ✅ Delete a club (only creator can)
            delete("{id}") {
                val id = call.parameters["id"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()

                if (id == null) {
                    return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing club ID"))
                }

                val db = FirebaseService.firestoreDb
                val docRef = db.collection("clubs").document(id)

                val snapshot = docRef.get().get()

                if (!snapshot.exists()) {
                    return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Club not found"))
                }

                val club = snapshot.toObject(Club::class.java)

                if (club == null || club.createdBy != userEmail) {
                    return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You are not authorized to delete this club"))
                }

                docRef.delete()
                call.respond(mapOf("message" to "Club deleted!", "deletedClubId" to id))
            }

        }
    }
}
