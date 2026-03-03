package com.example.studiora

import com.example.studiora.model.*
import com.example.studiora.viewmodel.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Studiora (Part 2) — run on the JVM, no device needed.
 * Covers: teacher attendance logic, admin analytics, model equality/copy,
 * schedule validation, and state transition flows.
 */
class StudiorUnitTest2 {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. TEACHER SELF-ATTENDANCE LOGIC TESTS
    // ─────────────────────────────────────────────────────────────────────────

    private fun teacherOverallPct(records: List<Pair<String, String>>): Float {
        if (records.isEmpty()) return 0f
        val present = records.count { it.second == "PRESENT" }
        return (present.toFloat() / records.size.toFloat()) * 100f
    }

    private fun teacherMonthly(records: List<Pair<String, String>>): Map<String, Float> {
        val byMonth = records.groupBy { it.first.take(7) }
        return byMonth.mapValues { (_, recs) ->
            val present = recs.count { it.second == "PRESENT" }
            if (recs.isEmpty()) 0f else (present.toFloat() / recs.size.toFloat()) * 100f
        }
    }

    private fun teacherRecentDays(records: List<Pair<String, String>>, days: Int = 10) =
        records.sortedByDescending { it.first }.take(days)

    @Test
    fun teacherOverallPct_empty_returnsZero() {
        assertEquals(0f, teacherOverallPct(emptyList()), 0.001f)
    }

    @Test
    fun teacherOverallPct_allPresent_returns100() {
        val records = listOf("2025-01-01" to "PRESENT", "2025-01-02" to "PRESENT")
        assertEquals(100f, teacherOverallPct(records), 0.001f)
    }

    @Test
    fun teacherOverallPct_allAbsent_returnsZero() {
        val records = listOf("2025-01-01" to "ABSENT", "2025-01-02" to "ABSENT")
        assertEquals(0f, teacherOverallPct(records), 0.001f)
    }

    @Test
    fun teacherOverallPct_60Percent() {
        val records = listOf(
            "2025-01-01" to "PRESENT",
            "2025-01-02" to "PRESENT",
            "2025-01-03" to "PRESENT",
            "2025-01-04" to "ABSENT",
            "2025-01-05" to "ABSENT"
        )
        assertEquals(60f, teacherOverallPct(records), 0.001f)
    }

    @Test
    fun teacherMonthly_groupsCorrectly() {
        val records = listOf(
            "2025-01-01" to "PRESENT",
            "2025-01-02" to "ABSENT",
            "2025-02-01" to "PRESENT",
            "2025-02-02" to "PRESENT"
        )
        val monthly = teacherMonthly(records)
        assertEquals(50f,  monthly["2025-01"]!!, 0.001f)
        assertEquals(100f, monthly["2025-02"]!!, 0.001f)
    }

    @Test
    fun teacherRecentDays_sortedDescending() {
        val records = listOf(
            "2025-01-03" to "PRESENT",
            "2025-01-01" to "ABSENT",
            "2025-01-05" to "PRESENT",
            "2025-01-02" to "ABSENT"
        )
        val recent = teacherRecentDays(records)
        assertEquals("2025-01-05", recent[0].first)
        assertEquals("2025-01-03", recent[1].first)
        assertEquals("2025-01-02", recent[2].first)
        assertEquals("2025-01-01", recent[3].first)
    }

    @Test
    fun teacherRecentDays_limitedToRequestedCount() {
        val records = (1..20).map { i -> "2025-01-${i.toString().padStart(2,'0')}" to "PRESENT" }
        assertEquals(5, teacherRecentDays(records, 5).size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. ADMIN ANALYTICS TESTS
    // ─────────────────────────────────────────────────────────────────────────

    private fun rec(
        id: String, studentId: String = "s1",
        status: String = "PRESENT", courseId: String = "", date: String = "2025-01-01"
    ) = AttendanceRecord(recordId = id, studentId = studentId, status = status,
        courseId = courseId, date = date, classId = "cls1", organizationId = "org1")

    private fun calculateAttendancePct(records: List<AttendanceRecord>): Float {
        if (records.isEmpty()) return 0f
        val present = records.count { it.status == "PRESENT" }
        return (present.toFloat() / records.size.toFloat()) * 100f
    }

    @Test
    fun allTeachersAttendance_filteredByOrgTeacherIds() {
        val allMap = mapOf(
            "t1" to listOf("2025-01-01" to "PRESENT", "2025-01-02" to "ABSENT"),
            "t2" to listOf("2025-01-01" to "PRESENT"),
            "t3" to listOf("2025-01-01" to "ABSENT") // NOT in this org
        )
        val orgTeacherIds = setOf("t1", "t2")
        val filtered = allMap
            .filter { (tid, _) -> tid in orgTeacherIds }
            .mapValues { (_, records) -> teacherOverallPct(records) }

        assertEquals(2, filtered.size)
        assertFalse(filtered.containsKey("t3"))
        assertEquals(50f,  filtered["t1"]!!, 0.001f)
        assertEquals(100f, filtered["t2"]!!, 0.001f)
    }

    @Test
    fun classAttendanceSummary_perStudentIsCorrect() {
        val records = listOf(
            rec("r1", studentId = "s1", status = "PRESENT", courseId = ""),
            rec("r2", studentId = "s1", status = "ABSENT",  courseId = ""),
            rec("r3", studentId = "s2", status = "PRESENT", courseId = ""),
            rec("r4", studentId = "s2", status = "PRESENT", courseId = "")
        )
        val classOnly = records.filter { it.courseId.isBlank() }
        val byStudent = classOnly.groupBy { it.studentId }
        val summary = byStudent.mapValues { (_, recs) -> calculateAttendancePct(recs) }

        assertEquals(50f,  summary["s1"]!!, 0.001f)
        assertEquals(100f, summary["s2"]!!, 0.001f)
    }

    @Test
    fun studentCountByClass_groupsCorrectly() {
        val students = listOf(
            User(uid = "s1", classId = "cls1", role = "STUDENT"),
            User(uid = "s2", classId = "cls1", role = "STUDENT"),
            User(uid = "s3", classId = "cls2", role = "STUDENT"),
            User(uid = "s4", classId = "cls1", role = "STUDENT")
        )
        val countByClass = students.groupBy { it.classId }.mapValues { it.value.size }
        assertEquals(3, countByClass["cls1"])
        assertEquals(1, countByClass["cls2"])
    }

    @Test
    fun courseCount_matchesNumberOfCourses() {
        val courses = listOf(
            Course(courseId = "c1", name = "Math"),
            Course(courseId = "c2", name = "Science"),
            Course(courseId = "c3", name = "English")
        )
        assertEquals(3, courses.size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. MODEL EQUALITY & COPY TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun course_equality_byAllFields() {
        val c1 = Course(courseId = "c1", name = "Math", classId = "cls1", teacherId = "t1", organizationId = "org1")
        val c2 = Course(courseId = "c1", name = "Math", classId = "cls1", teacherId = "t1", organizationId = "org1")
        assertEquals(c1, c2)
    }

    @Test
    fun course_copy_changesOnlyTargetField() {
        val course = Course(courseId = "c1", name = "Math", teacherId = "t1", classId = "cls1")
        val updated = course.copy(name = "Advanced Math")
        assertEquals("Advanced Math", updated.name)
        assertEquals("c1",  updated.courseId)
        assertEquals("t1",  updated.teacherId)
        assertEquals("cls1", updated.classId)
    }

    @Test
    fun scheduleItem_equality() {
        val s1 = ScheduleItem("Monday", "09:00", "10:00", "c1")
        val s2 = ScheduleItem("Monday", "09:00", "10:00", "c1")
        assertEquals(s1, s2)
    }

    @Test
    fun studyMaterial_copy_preservesFields() {
        val material = StudyMaterial(materialId = "m1", title = "Notes", courseId = "c1", uploadedBy = "t1")
        val updated = material.copy(title = "Updated Notes")
        assertEquals("Updated Notes", updated.title)
        assertEquals("m1", updated.materialId)
        assertEquals("c1", updated.courseId)
        assertEquals("t1", updated.uploadedBy)
    }

    @Test
    fun attendanceRecord_copy_changesStatus() {
        val record = AttendanceRecord(recordId = "r1", studentId = "s1", status = "ABSENT")
        val updated = record.copy(status = "PRESENT")
        assertEquals("PRESENT", updated.status)
        assertEquals("r1", updated.recordId)
        assertEquals("s1", updated.studentId)
    }

    @Test
    fun user_organizationId_preservedOnCopy() {
        val user = User(uid = "u1", name = "Alice", organizationId = "org1")
        val updated = user.copy(name = "Alicia")
        assertEquals("org1", updated.organizationId)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. SCHEDULE VALIDATION TESTS
    // ─────────────────────────────────────────────────────────────────────────

    private val validDays = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
    private fun isValidDay(day: String) = day in validDays
    private fun isValidTimeFormat(time: String) = time.matches(Regex("\\d{2}:\\d{2}"))

    @Test
    fun scheduleDay_validDays_pass() {
        validDays.forEach { assertTrue("$it should be valid", isValidDay(it)) }
    }

    @Test
    fun scheduleDay_invalidDay_fails() {
        assertFalse(isValidDay("Funday"))
        assertFalse(isValidDay(""))
        assertFalse(isValidDay("monday")) // case-sensitive
    }

    @Test
    fun scheduleTime_validFormat_passes() {
        assertTrue(isValidTimeFormat("09:00"))
        assertTrue(isValidTimeFormat("13:30"))
        assertTrue(isValidTimeFormat("23:59"))
    }

    @Test
    fun scheduleTime_invalidFormat_fails() {
        assertFalse(isValidTimeFormat("9:00"))   // missing leading zero
        assertFalse(isValidTimeFormat("09:0"))   // missing trailing digit
        assertFalse(isValidTimeFormat(""))
        assertFalse(isValidTimeFormat("9AM"))
    }

    @Test
    fun classSchedule_noDuplicateDays() {
        val schedule = listOf(
            ScheduleItem("Monday",    "09:00", "10:00", "c1"),
            ScheduleItem("Wednesday", "11:00", "12:00", "c2"),
            ScheduleItem("Friday",    "14:00", "15:00", "c3")
        )
        val days = schedule.map { it.day }
        assertEquals(days.size, days.distinct().size)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. STATE TRANSITION FLOW TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun authState_stateTransitions_areDistinct() {
        val states: List<AuthState> = listOf(
            AuthState.Idle, AuthState.Loading,
            AuthState.Success(User(uid = "u1")),
            AuthState.Error("err")
        )
        assertEquals(4, states.distinct().size)
    }

    @Test
    fun operationState_stateTransitions_areDistinct() {
        val states: List<OperationState> = listOf(
            OperationState.Idle, OperationState.Loading,
            OperationState.Success("ok"),
            OperationState.Error("err")
        )
        assertEquals(4, states.distinct().size)
    }

    @Test
    fun resetState_successIsDifferentFromIdle() {
        val idle: ResetState    = ResetState.Idle
        val success: ResetState = ResetState.Success
        assertNotEquals(idle, success)
    }

    @Test
    fun registerState_loadingIsDifferentFromSuccess() {
        val loading: RegisterState = RegisterState.Loading
        val success: RegisterState = RegisterState.Success(User(uid = "u1"))
        assertNotEquals(loading, success)
    }

    @Test
    fun profileUpdateState_allStatesDistinct() {
        val states: List<ProfileUpdateState> = listOf(
            ProfileUpdateState.Idle, ProfileUpdateState.Loading,
            ProfileUpdateState.Success, ProfileUpdateState.Error("err")
        )
        assertEquals(4, states.distinct().size)
    }
}

