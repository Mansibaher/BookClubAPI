package com.example.service

import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient

/**
 * Singleton service that provides access to the Firebase Firestore database.
 *
 * This abstraction allows centralized management of the Firestore instance, making it
 * easier to inject or mock for testing, and ensures consistent usage throughout the codebase.
 *
 * Usage:
 *   val db = FirebaseService.firestoreDb
 */
object FirebaseService {
    val firestoreDb: Firestore
        get() = FirestoreClient.getFirestore()
}
