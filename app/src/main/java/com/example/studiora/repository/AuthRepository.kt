package com.example.studiora.repository

import com.example.studiora.model.User
import com.example.studiora.model.UserRoles
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().reference

    fun getCurrentUser() = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Authentication failed")
            val uid = firebaseUser.uid

            val snapshot = database.child("users").child(uid).get().await()
            val user = snapshot.getValue(User::class.java)

            if (user != null) {
                // Normal case — user record exists
                Result.success(user)
            } else {
                // User exists in Auth but not in Database (e.g. created via Firebase Console)
                // Auto-create a record so they can log in — default role is ADMIN
                val newUser = User(
                    uid = uid,
                    email = firebaseUser.email ?: email,
                    name = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                        ?: email.substringBefore("@"),
                    role = UserRoles.ADMIN,
                    organizationId = uid   // org's own uid is its organizationId
                )
                database.child("users").child(uid).setValue(newUser).await()
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a user account WITHOUT disturbing the currently signed-in session.
     * Uses a secondary FirebaseApp so the admin/teacher stays logged in.
     */
    private suspend fun createUserWithSecondaryAuth(email: String, password: String): String {
        val secondaryAppName = "SecondaryApp"
        val secondaryApp = try {
            FirebaseApp.getInstance(secondaryAppName)
        } catch (e: IllegalStateException) {
            // Secondary app not initialized yet — create it using primary app's options
            val options = FirebaseApp.getInstance().options
            FirebaseApp.initializeApp(
                FirebaseApp.getInstance().applicationContext,
                options,
                secondaryAppName
            )
        }
        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
        val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("UID not found")
        secondaryAuth.signOut() // Sign out from secondary — primary session untouched
        return uid
    }

    suspend fun registerStudent(
        name: String,
        email: String,
        password: String,
        phone: String,
        classId: String,
        organizationId: String,
        addedByTeacherId: String = ""
    ): Result<User> {
        return try {
            val uid = createUserWithSecondaryAuth(email, password)
            val user = User(
                uid = uid,
                email = email,
                name = name,
                role = UserRoles.STUDENT,
                phone = phone,
                classId = classId,
                organizationId = organizationId
            )
            database.child("users").child(uid).setValue(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerTeacher(
        name: String,
        email: String,
        password: String,
        phone: String,
        subject: String,
        organizationId: String
    ): Result<User> {
        return try {
            val uid = createUserWithSecondaryAuth(email, password)
            val user = User(
                uid = uid,
                email = email,
                name = name,
                role = UserRoles.TEACHER,
                phone = phone,
                subject = subject,
                organizationId = organizationId
            )
            database.child("users").child(uid).setValue(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerOrganization(
        orgName: String,
        email: String,
        password: String,
        phone: String
    ): Result<User> {
        return try {
            // Organization registers itself — normal flow, no active session to protect
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("UID not found")
            val user = User(
                uid = uid,
                email = email,
                name = orgName,
                role = UserRoles.ADMIN,
                phone = phone,
                organizationId = uid   // org's own uid is its organizationId
            )
            database.child("users").child(uid).setValue(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(uid: String): Result<User> {
        return try {
            val snapshot = database.child("users").child(uid).get().await()
            val user = snapshot.getValue(User::class.java)
                ?: throw Exception("User not found")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
