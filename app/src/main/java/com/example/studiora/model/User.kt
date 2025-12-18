package com.example.studiora.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.STUDENT,
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    ADMIN,
    STUDENT
}

