package com.example.utils

import com.example.models.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

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
