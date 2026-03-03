package com.example.studiora.repository

import com.example.studiora.model.Class
import com.example.studiora.model.ScheduleItem
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class ClassRepository {
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun createClass(cls: Class): Result<Unit> {
        return try {
            database.child("classes").child(cls.classId).setValue(cls).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllClasses(organizationId: String): Result<List<Class>> {
        return try {
            val snapshot = database.child("classes").get().await()
            val classes = mutableListOf<Class>()
            snapshot.children.forEach { child ->
                val cls = child.getValue(Class::class.java)
                if (cls != null && cls.organizationId == organizationId) classes.add(cls)
            }
            Result.success(classes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClassesByTeacher(teacherId: String): Result<List<Class>> {
        return try {
            val snapshot = database.child("classes").get().await()
            val classes = mutableListOf<Class>()
            snapshot.children.forEach { child ->
                val cls = child.getValue(Class::class.java)
                if (cls != null && cls.teacherId == teacherId) classes.add(cls)
            }
            Result.success(classes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteClass(classId: String): Result<Unit> {
        return try {
            database.child("classes").child(classId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateClassTeacher(classId: String, newTeacherId: String, newTeacherName: String): Result<Unit> {
        return try {
            val updates = mapOf<String, Any>(
                "teacherId" to newTeacherId,
                "teacherName" to newTeacherName
            )
            database.child("classes").child(classId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateClassSchedule(classId: String, schedule: List<ScheduleItem>): Result<Unit> {
        return try {
            database.child("classes").child(classId).child("schedule").setValue(schedule).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClassById(classId: String): Result<Class> {
        return try {
            val snapshot = database.child("classes").child(classId).get().await()
            val cls = snapshot.getValue(Class::class.java)
                ?: throw Exception("Class not found")
            Result.success(cls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
