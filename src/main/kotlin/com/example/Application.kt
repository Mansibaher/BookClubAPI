package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.routes.authRoutes
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.gson.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.io.FileInputStream

// ✅ Import your route files
import com.example.routes.clubRoutes
import com.example.routes.bookRoutes
import com.example.routes.threadRoutes

/**
 * Main entry point of the Book Club API application.
 * Starts the Ktor server using the Netty engine.
 */
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

/**
 * Configures the main application module.
 * - Initializes Firebase
 * - Sets up JWT-based authentication
 * - Installs JSON serialization
 * - Registers all routing endpoints
 */
fun Application.module() {
    install(ContentNegotiation) {
        gson()
    }

    // ✅ Initialize Firebase
    val serviceAccount = FileInputStream("serviceAccountKey.json")
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()

    if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(options)
        println("✅ Firebase initialized")
    }

    // ✅ Install JWT Authentication
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "bookclub"
            verifier(
                JWT
                    .require(Algorithm.HMAC256("super_secret_key"))
                    .withIssuer("bookclub")
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("email").asString() != null)
                    JWTPrincipal(credential.payload)
                else null
            }
        }
    }

    routing {
        get("/") {
            call.respondText("Book Club API is running!")
        }

        // ✅ Public routes
        authRoutes()
        clubRoutes()
        bookRoutes()
        threadRoutes()

        // ✅ Protected routes
        authenticate("auth-jwt") {
            get("/protected") {
                val principal = call.principal<JWTPrincipal>()
                val email = principal!!.payload.getClaim("email").asString()
                call.respondText("🔐 Welcome, $email! This is a protected route.")
            }
        }
    }
}
