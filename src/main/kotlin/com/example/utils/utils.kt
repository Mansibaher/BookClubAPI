package com.example.utils

import com.example.models.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * A utility function that wraps a suspendable block in a try-catch
 * and returns a standardized [ApiResponse] to the client.
 *
 * @param call The [ApplicationCall] used to send the response.
 * @param onError The HTTP status to return in case of an exception (default: 500 Internal Server Error).
 * @param block A suspendable lambda that contains the main logic to execute.
 *
 * Usage:
 * ```
 * respondSafely(call) {
 *     // your logic here
 * }
 * ```
 *
 * This function helps avoid repetitive try-catch blocks and enforces
 * a consistent API response format across endpoints.
 */
suspend fun <T> respondSafely(
    call: ApplicationCall,
    onError: HttpStatusCode = HttpStatusCode.InternalServerError,
    block: suspend () -> T
) {
    try {
        val result = block()
        call.respond(ApiResponse(success = true, data = result))
    } catch (e: Exception) {
        call.respond(onError, ApiResponse<Unit>(false, error = e.localizedMessage))
    }
}
