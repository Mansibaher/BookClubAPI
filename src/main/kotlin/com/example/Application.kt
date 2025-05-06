package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.routing.authRoutes
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
import com.example.clubRoutes
import com.example.bookRoutes
import com.example.threadRoutes

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

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
        bookRoutes() // ✅ Added bookRoutes for Google Books API integration
        threadRoutes() // ✅ Added threadRoutes for discussion threads

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
