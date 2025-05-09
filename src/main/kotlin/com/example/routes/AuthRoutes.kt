package com.example.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.models.ApiResponse
import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class SignupRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)

private val jwtSecret = "super_secret_key" // For demo only. Use environment variable in production.

fun Route.authRoutes() {
    println("✅ /signup and /login routes registered")

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

    route("/login") {
        post {
            println("🔑 Received POST /login request")
            val request = call.receive<LoginRequest>()
            try {
                val firebaseToken = FirebaseAuth.getInstance()
                    .createCustomToken(request.email)

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
