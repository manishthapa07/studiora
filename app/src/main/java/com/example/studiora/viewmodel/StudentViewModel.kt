package com.example.studiora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studiora.model.AttendanceRecord
import com.example.studiora.model.Class
import com.example.studiora.model.Course
import com.example.studiora.model.StudyMaterial
import com.example.studiora.repository.AttendanceRepository
import com.example.studiora.repository.ClassRepository
import com.example.studiora.repository.CourseRepository
import com.example.studiora.repository.StudyMaterialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudentViewModel : ViewModel() {
    private val courseRepository = CourseRepository()
    private val materialRepository = StudyMaterialRepository()
    private val attendanceRepository = AttendanceRepository()
    private val classRepository = ClassRepository()

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _classSchedule = MutableStateFlow<Class?>(null)
    val classSchedule: StateFlow<Class?> = _classSchedule.asStateFlow()

    private val _materials = MutableStateFlow<List<StudyMaterial>>(emptyList())
    val materials: StateFlow<List<StudyMaterial>> = _materials.asStateFlow()

    // ALL records (used only for history display)
    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    // Class/day-level records only (courseId == "") — used for overall % and day stats
    private val _classAttendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val classAttendanceRecords: StateFlow<List<AttendanceRecord>> = _classAttendanceRecords.asStateFlow()

    // Overall % based ONLY on class-level records
    private val _attendancePercentage = MutableStateFlow(0f)
    val attendancePercentage: StateFlow<Float> = _attendancePercentage.asStateFlow()

    // Course-specific records only (courseId != ""), grouped by courseId
    private val _courseAttendanceMap = MutableStateFlow<Map<String, List<AttendanceRecord>>>(emptyMap())
    val courseAttendanceMap: StateFlow<Map<String, List<AttendanceRecord>>> = _courseAttendanceMap.asStateFlow()

    // Per-course attendance percentage: courseId -> %
    private val _courseAttendancePct = MutableStateFlow<Map<String, Float>>(emptyMap())
    val courseAttendancePct: StateFlow<Map<String, Float>> = _courseAttendancePct.asStateFlow()

    fun loadCoursesByClass(classId: String) {
        viewModelScope.launch {
            courseRepository.getCoursesByClass(classId).onSuccess { _courses.value = it }
        }
    }

    fun loadClassSchedule(classId: String) {
        viewModelScope.launch {
            classRepository.getClassById(classId).onSuccess { _classSchedule.value = it }
        }
    }

    fun loadMaterialsByCourse(courseId: String) {
        viewModelScope.launch {
            materialRepository.getMaterialsByCourse(courseId).onSuccess { _materials.value = it }
        }
    }

    fun loadAttendanceByStudent(studentId: String) {
        viewModelScope.launch {
            attendanceRepository.getAttendanceByStudent(studentId).onSuccess { all ->
                _attendanceRecords.value = all

                // ── Class/day-level attendance (courseId blank) ──────────────
                val classRecs = attendanceRepository.classOnlyRecords(all)
                _classAttendanceRecords.value = classRecs
                _attendancePercentage.value = attendanceRepository.calculateAttendancePercentage(classRecs)

                // ── Course-specific attendance (courseId non-blank) ──────────
                val courseRecs = attendanceRepository.courseOnlyRecords(all)
                val grouped = courseRecs.groupBy { it.courseId }
                _courseAttendanceMap.value = grouped
                _courseAttendancePct.value = grouped.mapValues { (_, recs) ->
                    attendanceRepository.calculateAttendancePercentage(recs)
                }
            }
        }
    }
}
