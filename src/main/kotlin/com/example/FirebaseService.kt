package com.example

import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient

object FirebaseService {
    val firestoreDb: Firestore
        get() = FirestoreClient.getFirestore()
}
