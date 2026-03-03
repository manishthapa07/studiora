package com.example.studiora.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "STUDENT",
    val phone: String = "",
    val subject: String = "",   // For teachers
    val classId: String = "",   // For students
    val profileImageUrl: String = "",
    val address: String = "",
    val parentName: String = "",
    val parentPhone: String = "",
    val documentUrl: String = "",
    val organizationId: String = "",  // UID of the ADMIN who owns this user
    val createdAt: Long = System.currentTimeMillis()
)

data class Class(
    val classId: String = "",
    val name: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val schedule: List<ScheduleItem> = emptyList(),
    val organizationId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ScheduleItem(
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val courseId: String = ""
)

data class Course(
    val courseId: String = "",
    val name: String = "",
    val description: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val organizationId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class StudyMaterial(
    val materialId: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "",
    val fileType: String = "PDF",
    val courseId: String = "",
    val uploadedBy: String = "",
    val uploaderName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class AttendanceRecord(
    val recordId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val classId: String = "",
    val courseId: String = "",       // empty = whole-class/day attendance; non-empty = course-specific
    val date: String = "",
    val status: String = "ABSENT",   // PRESENT or ABSENT
    val markedBy: String = "",
    val markedByName: String = "",
    val method: String = "MANUAL",   // MANUAL or QR_CODE
    val organizationId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

object UserRoles {
    const val ADMIN = "ADMIN"
    const val TEACHER = "TEACHER"
    const val STUDENT = "STUDENT"
}
