package com.example.models

/**
 * Data class representing a signup request payload.
 *
 * @property email The user's email address.
 * @property password The user's password.
 */
data class SignupRequest(val email: String, val password: String)

/**
 * Data class representing a login request payload.
 *
 * @property email The user's email address.
 * @property password The user's password (not verified here, as Firebase handles auth).
 */
data class LoginRequest(val email: String, val password: String)
