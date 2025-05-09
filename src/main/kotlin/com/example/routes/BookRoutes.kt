package com.example.routes

import com.example.client
import com.example.models.Book
import com.example.models.ApiResponse
import com.google.gson.JsonParser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Defines routes for interacting with external book search APIs.
 *
 * Route:
 *   GET /books/search?query=your_query
 *     - Calls the Google Books API to fetch book information based on the query.
 *     - Returns a list of simplified Book models.
 *
 * Example request:
 *   GET /books/search?query=Harry+Potter
 *
 * Returns:
 *   - 200 OK with list of books if successful.
 *   - 400 Bad Request if query parameter is missing.
 *   - 500 Internal Server Error if the external API call fails.
 */
fun Route.bookRoutes() {
    route("/books") {
        get("/search") {
            val query = call.request.queryParameters["query"]
            if (query.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<List<Book>>(success = false, error = "Missing search query")
                )
                return@get
            }
            // Call Google Books API
            try {
                val response: HttpResponse = client.get("https://www.googleapis.com/books/v1/volumes") {
                    url {
                        parameters.append("q", query)
                        parameters.append("maxResults", "10")
                    }
                }
                // Parse and convert each book entry to our Book model
                val rawJson = response.bodyAsText()
                val json = JsonParser.parseString(rawJson).asJsonObject
                val items = json.getAsJsonArray("items")

                val books = items.map { item ->
                    val volumeInfo = item.asJsonObject.getAsJsonObject("volumeInfo")

                    val title = volumeInfo.get("title")?.asString ?: "No Title"
                    val authors = volumeInfo.getAsJsonArray("authors")?.map { it.asString } ?: listOf("Unknown Author")
                    val thumbnail = volumeInfo.getAsJsonObject("imageLinks")?.get("thumbnail")?.asString

                    Book(title, authors, thumbnail)
                }

                call.respond(ApiResponse(success = true, data = books))

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<List<Book>>(success = false, error = "Google Books API error: ${e.message}")
                )
            }
        }
    }
}
