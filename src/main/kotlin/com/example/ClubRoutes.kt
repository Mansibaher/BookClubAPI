package com.example

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

        // Public: List all clubs
        get {
            call.respond(clubs)
        }

        // Authenticated: Create + Join + Leave + Delete
        authenticate("auth-jwt") {

            // ✅ Create a club
            post {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal!!.payload.getClaim("email").asString()

                val request = call.receive<ClubRequest>()

                val club = Club(
                    id = UUID.randomUUID().toString(),
                    name = request.name,
                    description = request.description,
                    createdBy = userId,
                    members = mutableListOf(userId)
                )

                clubs.add(club)
                call.respond(mapOf("message" to "Club created!", "club" to club))
            }

            // ✅ Join a club
            post("{id}/join") {
                val id = call.parameters["id"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val club = clubs.find { it.id == id }

                if (club != null) {
                    if (!club.members.contains(userEmail)) {
                        club.members.add(userEmail)
                        call.respond(mapOf("message" to "Joined club!", "club" to club))
                    } else {
                        call.respond(mapOf("message" to "Already a member"))
                    }
                } else {
                    call.respond(mapOf("error" to "Club not found"))
                }
            }

            // ✅ Leave a club
            delete("{id}/leave") {
                val id = call.parameters["id"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val club = clubs.find { it.id == id }

                if (club != null) {
                    if (club.members.contains(userEmail)) {
                        club.members.remove(userEmail)
                        call.respond(mapOf("message" to "Left the club", "clubId" to club.id))
                    } else {
                        call.respond(mapOf("message" to "You are not a member of this club"))
                    }
                } else {
                    call.respond(mapOf("error" to "Club not found"))
                }
            }

            // ✅ Delete a club (only creator can)
            delete("{id}") {
                val id = call.parameters["id"]
                val userEmail = call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
                val club = clubs.find { it.id == id }

                if (club != null) {
                    if (club.createdBy == userEmail) {
                        clubs.remove(club)
                        call.respond(mapOf("message" to "Club deleted!", "deletedClubId" to club.id))
                    } else {
                        call.respond(mapOf("error" to "You are not authorized to delete this club"))
                    }
                } else {
                    call.respond(mapOf("error" to "Club not found"))
                }
            }
        }
    }
}
