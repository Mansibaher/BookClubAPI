package com.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*

/**
* Global HTTP client configured with the CIO engine and Gson for JSON parsing.
*
* This client is used for making external HTTP requests (e.g., Google Books API).
* The [ContentNegotiation] plugin is installed to automatically handle JSON (de)serialization.
*
* Note:
* - Reuse this client throughout the app to avoid socket exhaustion.
* - Consider adding timeout settings or logging plugins for production.
*/
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        gson()
    }
}
