package com.example.studiora.repository

import com.example.studiora.model.User
import com.example.studiora.model.UserRoles
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun getAllTeachers(organizationId: String): Result<List<User>> {
        return try {
            val snapshot = database.child("users").get().await()
            val teachers = mutableListOf<User>()
            snapshot.children.forEach { child ->
                val user = child.getValue(User::class.java)
                if (user != null && user.role == UserRoles.TEACHER && user.organizationId == organizationId) {
                    teachers.add(user)
                }
            }
            Result.success(teachers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllStudents(organizationId: String): Result<List<User>> {
        return try {
            val snapshot = database.child("users").get().await()
            val students = mutableListOf<User>()
            snapshot.children.forEach { child ->
                val user = child.getValue(User::class.java)
                if (user != null && user.role == UserRoles.STUDENT && user.organizationId == organizationId) {
                    students.add(user)
                }
            }
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudentsByClass(classId: String): Result<List<User>> {
        return try {
            val snapshot = database.child("users").get().await()
            val students = mutableListOf<User>()
            snapshot.children.forEach { child ->
                val user = child.getValue(User::class.java)
                if (user != null && user.role == UserRoles.STUDENT && user.classId == classId) {
                    students.add(user)
                }
            }
            Result.success(students)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(uid: String): Result<Unit> {
        return try {
            database.child("users").child(uid).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            database.child("users").child(user.uid).setValue(user).await()
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
}
