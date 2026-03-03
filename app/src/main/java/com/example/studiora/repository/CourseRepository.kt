package com.example.studiora.repository

import com.example.studiora.model.Course
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class CourseRepository {
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun addCourse(course: Course): Result<Unit> {
        return try {
            database.child("courses").child(course.courseId).setValue(course).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllCourses(organizationId: String): Result<List<Course>> {
        return try {
            val snapshot = database.child("courses").get().await()
            val courses = mutableListOf<Course>()
            snapshot.children.forEach { child ->
                val course = child.getValue(Course::class.java)
                if (course != null && course.organizationId == organizationId) courses.add(course)
            }
            Result.success(courses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCoursesByClass(classId: String): Result<List<Course>> {
        return try {
            val snapshot = database.child("courses").get().await()
            val courses = mutableListOf<Course>()
            snapshot.children.forEach { child ->
                val course = child.getValue(Course::class.java)
                if (course != null && course.classId == classId) courses.add(course)
            }
            Result.success(courses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCoursesByTeacher(teacherId: String): Result<List<Course>> {
        return try {
            val snapshot = database.child("courses").get().await()
            val courses = mutableListOf<Course>()
            snapshot.children.forEach { child ->
                val course = child.getValue(Course::class.java)
                if (course != null && course.teacherId == teacherId) courses.add(course)
            }
            Result.success(courses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCourse(courseId: String): Result<Unit> {
        return try {
            database.child("courses").child(courseId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
