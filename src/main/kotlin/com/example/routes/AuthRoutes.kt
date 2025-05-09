package com.example.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.ApiResponse
import com.example.models.LoginRequest
import com.example.models.SignupRequest
import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val jwtSecret = "super_secret_key"

/**
 * Defines authentication routes for user signup and login.
 *
 * Routes:
 *  - POST /signup: Creates a new Firebase user account.
 *  - POST /login: Generates a JWT token using a Firebase custom token.
 *
 * Responses are wrapped in a standard ApiResponse<T> format.
 */
fun Route.authRoutes() {

    // Route to handle user registration
    route("/signup") {
        post {
            println("📩 Received POST /signup request")
            val request = call.receive<SignupRequest>()
            try {
                val user = FirebaseAuth.getInstance().createUser(
                    com.google.firebase.auth.UserRecord.CreateRequest()
                        .setEmail(request.email)
                        .setPassword(request.password)
                )
                val response = mapOf("uid" to user.uid, "email" to user.email)
                call.respond(ApiResponse(success = true, data = response))
            } catch (e: Exception) {
                call.respond(ApiResponse<Map<String, String>>(success = false, error = e.localizedMessage))
            }
        }
    }

    // Route to handle login and JWT issuance
    route("/login") {
        post {
            println("🔑 Received POST /login request")
            val request = call.receive<LoginRequest>()
            try {
                // Firebase custom token creation (used for client-side Firebase auth)
                val firebaseToken = FirebaseAuth.getInstance()
                    .createCustomToken(request.email)
                // Generate backend JWT for authenticated access to server endpoints
                val jwt = JWT.create()
                    .withIssuer("bookclub")
                    .withClaim("email", request.email)
                    .sign(Algorithm.HMAC256(jwtSecret))

                val response = mapOf("token" to jwt)
                call.respond(ApiResponse(success = true, data = response))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Map<String, String>>(success = false, error = "Login failed: ${e.localizedMessage}")
                )
            }
        }

    }
}
