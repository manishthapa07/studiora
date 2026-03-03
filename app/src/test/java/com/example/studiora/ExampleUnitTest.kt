package com.example.studiora

import com.example.studiora.model.*
import com.example.studiora.viewmodel.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Studiora — run on the JVM, no device needed.
 * Covers: data models, attendance calculation logic, sealed state classes, and input validation.
 */
class StudiorUnitTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. DATA MODEL TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun user_defaultRole_isStudent() {
        val user = User(uid = "u1", email = "a@b.com", name = "Alice")
        assertEquals("STUDENT", user.role)
    }

    @Test
    fun user_copy_preservesUnchangedFields() {
        val user = User(uid = "u1", email = "a@b.com", name = "Alice", role = "TEACHER", phone = "0123456789")
        val updated = user.copy(name = "Alicia")
        assertEquals("Alicia", updated.name)
        assertEquals("u1", updated.uid)
        assertEquals("TEACHER", updated.role)
        assertEquals("0123456789", updated.phone)
    }

    @Test
    fun userRoles_constants_areCorrect() {
        assertEquals("ADMIN", UserRoles.ADMIN)
        assertEquals("TEACHER", UserRoles.TEACHER)
        assertEquals("STUDENT", UserRoles.STUDENT)
    }

    @Test
    fun attendanceRecord_defaultStatus_isAbsent() {
        val record = AttendanceRecord(recordId = "r1")
        assertEquals("ABSENT", record.status)
    }

    @Test
    fun attendanceRecord_defaultMethod_isManual() {
        val record = AttendanceRecord(recordId = "r1")
        assertEquals("MANUAL", record.method)
    }

    @Test
    fun studyMaterial_defaultFileType_isPDF() {
        val material = StudyMaterial(materialId = "m1", title = "Lecture 1")
        assertEquals("PDF", material.fileType)
    }

    @Test
    fun course_defaultValues_areEmptyStrings() {
        val course = Course()
        assertEquals("", course.courseId)
        assertEquals("", course.classId)
        assertEquals("", course.teacherId)
    }

    @Test
    fun classModel_defaultSchedule_isEmpty() {
        val cls = Class()
        assertTrue(cls.schedule.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. ATTENDANCE CALCULATION LOGIC TESTS
    // ─────────────────────────────────────────────────────────────────────────

    private fun calculateAttendancePct(records: List<AttendanceRecord>): Float {
        if (records.isEmpty()) return 0f
        val present = records.count { it.status == "PRESENT" }
        return (present.toFloat() / records.size.toFloat()) * 100f
    }

    private fun classOnlyRecords(records: List<AttendanceRecord>) =
        records.filter { it.courseId.isBlank() }

    private fun courseOnlyRecords(records: List<AttendanceRecord>) =
        records.filter { it.courseId.isNotBlank() }

    private fun rec(id: String, status: String = "PRESENT", courseId: String = "", date: String = "2025-01-01") =
        AttendanceRecord(recordId = id, studentId = "s1", status = status, courseId = courseId, date = date, classId = "cls1")

    @Test
    fun attendancePct_emptyList_returnsZero() {
        assertEquals(0f, calculateAttendancePct(emptyList()), 0.001f)
    }

    @Test
    fun attendancePct_allPresent_returns100() {
        val records = listOf(rec("r1"), rec("r2"), rec("r3"))
        assertEquals(100f, calculateAttendancePct(records), 0.001f)
    }

    @Test
    fun attendancePct_allAbsent_returnsZero() {
        val records = listOf(rec("r1", "ABSENT"), rec("r2", "ABSENT"))
        assertEquals(0f, calculateAttendancePct(records), 0.001f)
    }

    @Test
    fun attendancePct_halfPresent_returns50() {
        val records = listOf(rec("r1", "PRESENT"), rec("r2", "ABSENT"), rec("r3", "PRESENT"), rec("r4", "ABSENT"))
        assertEquals(50f, calculateAttendancePct(records), 0.001f)
    }

    @Test
    fun classOnlyRecords_filtersBlankCourseId() {
        val all = listOf(rec("r1", courseId = ""), rec("r2", courseId = "math"), rec("r3", courseId = ""))
        assertEquals(2, classOnlyRecords(all).size)
        assertTrue(classOnlyRecords(all).all { it.courseId.isBlank() })
    }

    @Test
    fun courseOnlyRecords_filtersNonBlankCourseId() {
        val all = listOf(rec("r1", courseId = ""), rec("r2", courseId = "math"), rec("r3", courseId = "sci"))
        assertEquals(2, courseOnlyRecords(all).size)
        assertTrue(courseOnlyRecords(all).all { it.courseId.isNotBlank() })
    }

    @Test
    fun overallPct_usesOnlyClassLevelRecords() {
        // 1 present + 1 absent class-level, plus 2 course-level absents (should be ignored)
        val all = listOf(
            rec("r1", "PRESENT", courseId = ""),
            rec("r2", "ABSENT",  courseId = ""),
            rec("r3", "ABSENT",  courseId = "math"),
            rec("r4", "ABSENT",  courseId = "math")
        )
        val pct = calculateAttendancePct(classOnlyRecords(all))
        assertEquals(50f, pct, 0.001f)
    }

    @Test
    fun courseAttendance_groupsByCorrectCourse() {
        val records = listOf(
            rec("r1", "PRESENT", courseId = "math"),
            rec("r2", "ABSENT",  courseId = "math"),
            rec("r3", "PRESENT", courseId = "sci"),
            rec("r4", "PRESENT", courseId = "sci")
        )
        val grouped = records.groupBy { it.courseId }
        assertEquals(50f,  calculateAttendancePct(grouped["math"]!!), 0.001f)
        assertEquals(100f, calculateAttendancePct(grouped["sci"]!!),  0.001f)
    }

    @Test
    fun monthlyAttendance_groupsByYYYYMM() {
        val records = listOf(
            rec("r1", "PRESENT", date = "2025-01-01"),
            rec("r2", "ABSENT",  date = "2025-01-15"),
            rec("r3", "PRESENT", date = "2025-02-01")
        )
        val byMonth = records.groupBy { it.date.take(7) }
        assertEquals(50f,  calculateAttendancePct(byMonth["2025-01"]!!), 0.001f)
        assertEquals(100f, calculateAttendancePct(byMonth["2025-02"]!!), 0.001f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. SEALED STATE CLASS TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun authState_idle_isNotLoadingOrError() {
        val state: AuthState = AuthState.Idle
        assertTrue(state is AuthState.Idle)
        assertFalse(state is AuthState.Loading)
        assertFalse(state is AuthState.Error)
    }

    @Test
    fun authState_success_holdsUser() {
        val user = User(uid = "u1", name = "Bob")
        val state = AuthState.Success(user)
        assertEquals(user, state.user)
    }

    @Test
    fun authState_error_holdsMessage() {
        val state = AuthState.Error("Invalid credentials")
        assertEquals("Invalid credentials", state.message)
    }

    @Test
    fun operationState_success_defaultMessageIsEmpty() {
        val state = OperationState.Success()
        assertEquals("", state.message)
    }

    @Test
    fun operationState_error_holdsMessage() {
        val state = OperationState.Error("Failed to delete")
        assertEquals("Failed to delete", state.message)
    }

    @Test
    fun registerState_error_holdsMessage() {
        val state = RegisterState.Error("Email already in use")
        assertEquals("Email already in use", state.message)
    }

    @Test
    fun resetState_error_holdsMessage() {
        val state = ResetState.Error("User not found")
        assertEquals("User not found", state.message)
    }

    @Test
    fun profileUpdateState_success_isDistinctFromIdle() {
        val idle: ProfileUpdateState = ProfileUpdateState.Idle
        val success: ProfileUpdateState = ProfileUpdateState.Success
        assertNotEquals(idle, success)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. INPUT VALIDATION LOGIC TESTS
    // ─────────────────────────────────────────────────────────────────────────

    private fun isValidEmail(email: String): Boolean {
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return email.isNotBlank() && regex.matches(email)
    }

    private fun isValidPassword(pw: String) = pw.length >= 6

    private fun isValidPhone(phone: String) = phone.matches(Regex("\\d{10,}"))

    private fun isValidAttendanceStatus(status: String) = status == "PRESENT" || status == "ABSENT"

    private fun isValidDateFormat(date: String) = date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))

    @Test
    fun email_validFormat_passes() {
        assertTrue(isValidEmail("user@example.com"))
        assertTrue(isValidEmail("user.name@domain.org"))
    }

    @Test
    fun email_invalidFormat_fails() {
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("notanemail"))
        assertFalse(isValidEmail("missing@"))
    }

    @Test
    fun password_lessThan6Chars_isInvalid() {
        assertFalse(isValidPassword("12345"))
        assertFalse(isValidPassword(""))
    }

    @Test
    fun password_6OrMoreChars_isValid() {
        assertTrue(isValidPassword("123456"))
        assertTrue(isValidPassword("securePass!"))
    }

    @Test
    fun phone_10Digits_isValid() {
        assertTrue(isValidPhone("0123456789"))
    }

    @Test
    fun phone_lessThan10Digits_isInvalid() {
        assertFalse(isValidPhone("12345"))
        assertFalse(isValidPhone(""))
    }

    @Test
    fun attendanceStatus_presentAndAbsent_areValid() {
        assertTrue(isValidAttendanceStatus("PRESENT"))
        assertTrue(isValidAttendanceStatus("ABSENT"))
        assertFalse(isValidAttendanceStatus("LATE"))
        assertFalse(isValidAttendanceStatus(""))
    }

    @Test
    fun dateFormat_yyyyMMdd_isValid() {
        assertTrue(isValidDateFormat("2025-01-15"))
        assertFalse(isValidDateFormat("15-01-2025"))
        assertFalse(isValidDateFormat(""))
    }
}