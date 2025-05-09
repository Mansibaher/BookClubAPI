# 📚 Book Club API

## 🌟 Introduction

The Book Club API is a Kotlin-based backend application developed using the Ktor framework. It provides functionalities to manage book clubs, user authentication, and book discussions. The API is designed to integrate with Firebase for authentication and data storage, and it leverages JWT for secure API access.

## ✨ Features

* 🔑 User Authentication with JWT
* ☁️ Firebase Integration for data storage
* 📖 Book Club Management (create, join, leave, delete)
* 🔍 Book Search using Google Books API
* 💬 Discussion Threads within Clubs
* 🔒 Protected Routes for authorized users

## 🛠️ Tech Stack

* **Backend:** Kotlin, Ktor
* **Authentication:** Firebase Auth, JWT
* **Database:** Firebase Firestore
* **External APIs:** Google Books API
* **Build Tool:** Gradle

## ✅ Prerequisites

* Java 17+
* Gradle 8.10+
* Firebase Service Account Key

## 🚀 Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/bookclubapi.git
   ```
2. Navigate to the project directory:

   ```bash
   cd bookclubapi
   ```
3. Configure Firebase credentials:

   * Place your Firebase service account key as `serviceAccountKey.json` in the root directory.

## ▶️ Running the Application

1. Run the application using Gradle:

   ```bash
   ./gradlew run
   ```
2. Access the API at:

   ```
   http://localhost:8080/
   ```

## 🌐 API Endpoints

### 🌍 Public Endpoints

* `GET /` - Health Check
* `POST /signup` - User Signup
* `POST /login` - User Login
* `GET /books/search` - Search Books by Query

### 🔐 Protected Endpoints

* `GET /protected` - Access a protected route
* `POST /clubs` - Create a new club
* `POST /clubs/{id}/join` - Join a club
* `DELETE /clubs/{id}/leave` - Leave a club
* `PATCH /clubs/{id}/currentBook` - Update current book in a club
* `DELETE /clubs/{id}` - Delete a club
* `POST /clubs/{clubId}/threads` - Create a new discussion thread
* `POST /clubs/{clubId}/threads/{threadId}/comments` - Add a comment to a thread

## 🛑 Error Handling

All API responses follow a structured format:

```
{
  "success": true/false,
  "data": {...},
  "error": "Error message if any"
}
```

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request for any improvements.

## 📄 License

This project is licensed under the Apache License 2.0.
