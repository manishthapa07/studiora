package com.example.studiora.repository

import com.example.studiora.model.AttendanceRecord
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AttendanceRepository {
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun markAttendance(record: AttendanceRecord): Result<Unit> {
        return try {
            database.child("attendance").child(record.recordId).setValue(record).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markBulkAttendance(records: List<AttendanceRecord>): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            records.forEach { record ->
                updates["attendance/${record.recordId}"] = record
            }
            database.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceByStudent(studentId: String): Result<List<AttendanceRecord>> {
        return try {
            val snapshot = database.child("attendance").get().await()
            val records = mutableListOf<AttendanceRecord>()
            snapshot.children.forEach { child ->
                val record = child.getValue(AttendanceRecord::class.java)
                if (record != null && record.studentId == studentId) records.add(record)
            }
            Result.success(records.sortedByDescending { it.date })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Separation helpers ───────────────────────────────────────────────────
    /** Records where courseId is blank = whole-class / day-level attendance */
    fun classOnlyRecords(records: List<AttendanceRecord>) = records.filter { it.courseId.isBlank() }

    /** Records where courseId is set = course-specific attendance */
    fun courseOnlyRecords(records: List<AttendanceRecord>) = records.filter { it.courseId.isNotBlank() }

    suspend fun getAttendanceByClassAndDate(classId: String, date: String): Result<List<AttendanceRecord>> {
        return try {
            val snapshot = database.child("attendance").get().await()
            val records = mutableListOf<AttendanceRecord>()
            snapshot.children.forEach { child ->
                val record = child.getValue(AttendanceRecord::class.java)
                if (record != null && record.classId == classId && record.date == date) records.add(record)
            }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetch attendance filtered by classId + courseId + date (course-specific)
    suspend fun getAttendanceByCourseAndDate(classId: String, courseId: String, date: String): Result<List<AttendanceRecord>> {
        return try {
            val snapshot = database.child("attendance").get().await()
            val records = mutableListOf<AttendanceRecord>()
            snapshot.children.forEach { child ->
                val record = child.getValue(AttendanceRecord::class.java)
                if (record != null && record.classId == classId && record.courseId == courseId && record.date == date)
                    records.add(record)
            }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetch all attendance for a course (for summary)
    suspend fun getAttendanceByCourse(courseId: String): Result<List<AttendanceRecord>> {
        return try {
            val snapshot = database.child("attendance").get().await()
            val records = mutableListOf<AttendanceRecord>()
            snapshot.children.forEach { child ->
                val record = child.getValue(AttendanceRecord::class.java)
                if (record != null && record.courseId == courseId) records.add(record)
            }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAttendanceByClass(classId: String): Result<List<AttendanceRecord>> {
        return try {
            val snapshot = database.child("attendance").get().await()
            val records = mutableListOf<AttendanceRecord>()
            snapshot.children.forEach { child ->
                val record = child.getValue(AttendanceRecord::class.java)
                if (record != null && record.classId == classId) records.add(record)
            }
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllAttendance(): Result<List<AttendanceRecord>> {
        return try {
            val snapshot = database.child("attendance").get().await()
            val records = mutableListOf<AttendanceRecord>()
            snapshot.children.forEach { child ->
                val record = child.getValue(AttendanceRecord::class.java)
                if (record != null) records.add(record)
            }
            Result.success(records.sortedByDescending { it.date })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun calculateAttendancePercentage(records: List<AttendanceRecord>): Float {
        if (records.isEmpty()) return 0f
        val present = records.count { it.status == "PRESENT" }
        return (present.toFloat() / records.size.toFloat()) * 100f
    }

    fun calculateMonthlyAttendance(records: List<AttendanceRecord>): Map<String, Float> {
        // date format: "yyyy-MM-dd"
        val byMonth = records.groupBy { it.date.take(7) } // "yyyy-MM"
        return byMonth.mapValues { (_, recs) -> calculateAttendancePercentage(recs) }
            .toSortedMap(reverseOrder())
    }

    fun getRecentDaysAttendance(records: List<AttendanceRecord>, days: Int = 10): List<Pair<String, String>> {
        // Returns list of (date, status) sorted desc, last `days` unique dates
        val byDate = records.groupBy { it.date }
        return byDate.entries
            .sortedByDescending { it.key }
            .take(days)
            .map { (date, recs) ->
                val status = if (recs.any { it.status == "PRESENT" }) "PRESENT" else "ABSENT"
                date to status
            }
    }

    // ─── Teacher Self-Attendance ──────────────────────────────────────────────

    suspend fun markTeacherAttendance(teacherId: String, date: String, status: String): Result<Unit> {
        return try {
            val key = "${teacherId}_${date}"
            val record = mapOf(
                "teacherId" to teacherId,
                "date" to date,
                "status" to status,
                "markedAt" to System.currentTimeMillis()
            )
            database.child("teacher_attendance").child(key).setValue(record).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeacherAttendance(teacherId: String): Result<List<Pair<String, String>>> {
        return try {
            val snapshot = database.child("teacher_attendance").get().await()
            val records = mutableListOf<Pair<String, String>>()
            snapshot.children.forEach { child ->
                val tid = child.child("teacherId").getValue(String::class.java) ?: return@forEach
                val date = child.child("date").getValue(String::class.java) ?: return@forEach
                val status = child.child("status").getValue(String::class.java) ?: return@forEach
                if (tid == teacherId) records.add(date to status)
            }
            Result.success(records.sortedByDescending { it.first })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllTeacherAttendance(): Result<Map<String, List<Pair<String, String>>>> {
        return try {
            val snapshot = database.child("teacher_attendance").get().await()
            val map = mutableMapOf<String, MutableList<Pair<String, String>>>()
            snapshot.children.forEach { child ->
                val tid = child.child("teacherId").getValue(String::class.java) ?: return@forEach
                val date = child.child("date").getValue(String::class.java) ?: return@forEach
                val status = child.child("status").getValue(String::class.java) ?: return@forEach
                map.getOrPut(tid) { mutableListOf() }.add(date to status)
            }
            Result.success(map)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun calculateTeacherMonthlyAttendance(records: List<Pair<String, String>>): Map<String, Float> {
        val byMonth = records.groupBy { it.first.take(7) }
        return byMonth.mapValues { (_, recs) ->
            val present = recs.count { it.second == "PRESENT" }
            if (recs.isEmpty()) 0f else (present.toFloat() / recs.size.toFloat()) * 100f
        }.toSortedMap(reverseOrder())
    }

    fun calculateTeacherOverallPct(records: List<Pair<String, String>>): Float {
        if (records.isEmpty()) return 0f
        val present = records.count { it.second == "PRESENT" }
        return (present.toFloat() / records.size.toFloat()) * 100f
    }

    fun getTeacherRecentDays(records: List<Pair<String, String>>, days: Int = 10): List<Pair<String, String>> {
        return records.sortedByDescending { it.first }.take(days)
    }
}

