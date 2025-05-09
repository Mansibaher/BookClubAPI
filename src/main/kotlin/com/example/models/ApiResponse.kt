package com.example.models

/**
 * A generic wrapper class for API responses.
 *
 * @param T The type of the data being returned in the response.
 * @property success Indicates whether the API call was successful.
 * @property data The response payload of type [T], present only when the call is successful.
 * @property error A descriptive error message, present only when the call is not successful.
 *
 * This class provides a consistent structure for handling API results,
 * allowing clients to easily distinguish between success and failure cases.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)
