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
 * Defining routes for interacting with external book search APIs.
 *
 * Route:
 *   GET /books/search?query=your_query&page=1&limit=10
 *     - Calls the Google Books API to fetch book information based on the query.
 *     - Returns a list of simplified Book models.
 */
fun Route.bookRoutes() {
    route("/books") {
        get("/search") {
            val query = call.request.queryParameters["query"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            if (query.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<List<Book>>(success = false, error = "Missing search query")
                )
                return@get
            }

            val startIndex = (page - 1) * limit

            try {
                // Calling the Google Books API with pagination parameters
                val response: HttpResponse = client.get("https://www.googleapis.com/books/v1/volumes") {
                    url {
                        parameters.append("q", query)
                        parameters.append("maxResults", limit.toString())
                        parameters.append("startIndex", startIndex.toString())
                    }
                }

                // Reading the raw response
                val rawJson = response.bodyAsText()
                
                if (rawJson.isBlank()) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<List<Book>>(success = false, error = "Empty response from Google Books API")
                    )
                    return@get
                }

                val json = JsonParser.parseString(rawJson).asJsonObject

                if (!json.has("items") || json.getAsJsonArray("items").size() == 0) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<List<Book>>(success = false, error = "No books found for the given query")
                    )
                    return@get
                }

                // Extracting and converting each book entry to our Book model
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
                    ApiResponse<List<Book>>(success = false, error = "Google Books API error: ${e.localizedMessage ?: "Unknown error"}")
                )
            }
        }
    }
}
