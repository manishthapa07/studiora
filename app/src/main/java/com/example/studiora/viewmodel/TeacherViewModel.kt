package com.example.studiora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studiora.model.AttendanceRecord
import com.example.studiora.model.Course
import com.example.studiora.model.StudyMaterial
import com.example.studiora.model.User
import com.example.studiora.repository.AttendanceRepository
import com.example.studiora.repository.AuthRepository
import com.example.studiora.repository.ClassRepository
import com.example.studiora.repository.CourseRepository
import com.example.studiora.repository.StudyMaterialRepository
import com.example.studiora.repository.UserRepository
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TeacherViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val classRepository = ClassRepository()
    private val courseRepository = CourseRepository()
    private val materialRepository = StudyMaterialRepository()
    private val attendanceRepository = AttendanceRepository()

    private val _students = MutableStateFlow<List<User>>(emptyList())
    val students: StateFlow<List<User>> = _students.asStateFlow()

    private val _classes = MutableStateFlow<List<com.example.studiora.model.Class>>(emptyList())
    val classes: StateFlow<List<com.example.studiora.model.Class>> = _classes.asStateFlow()

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _materials = MutableStateFlow<List<StudyMaterial>>(emptyList())
    val materials: StateFlow<List<StudyMaterial>> = _materials.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private val _todayAttendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val todayAttendanceRecords: StateFlow<List<AttendanceRecord>> = _todayAttendanceRecords.asStateFlow()

    private val _todayAttendanceLoaded = MutableStateFlow(false)
    val todayAttendanceLoaded: StateFlow<Boolean> = _todayAttendanceLoaded.asStateFlow()

    // Attendance analytics for teacher's class
    private val _classAttendanceSummary = MutableStateFlow<Map<String, Float>>(emptyMap()) // studentId -> overall%
    val classAttendanceSummary: StateFlow<Map<String, Float>> = _classAttendanceSummary.asStateFlow()

    // Course-specific attendance summary: studentId -> overall% for the current course
    private val _courseAttendanceSummary = MutableStateFlow<Map<String, Float>>(emptyMap())
    val courseAttendanceSummary: StateFlow<Map<String, Float>> = _courseAttendanceSummary.asStateFlow()

    private val _selectedStudentAttendance = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val selectedStudentAttendance: StateFlow<List<AttendanceRecord>> = _selectedStudentAttendance.asStateFlow()

    private val _selectedStudentMonthly = MutableStateFlow<Map<String, Float>>(emptyMap())
    val selectedStudentMonthly: StateFlow<Map<String, Float>> = _selectedStudentMonthly.asStateFlow()

    private val _selectedStudentRecentDays = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val selectedStudentRecentDays: StateFlow<List<Pair<String, String>>> = _selectedStudentRecentDays.asStateFlow()

    private val _selectedStudentOverallPct = MutableStateFlow(0f)
    val selectedStudentOverallPct: StateFlow<Float> = _selectedStudentOverallPct.asStateFlow()

    // Teacher's own self-attendance
    private val _ownAttendanceRecords = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val ownAttendanceRecords: StateFlow<List<Pair<String, String>>> = _ownAttendanceRecords.asStateFlow()

    private val _ownOverallPct = MutableStateFlow(0f)
    val ownOverallPct: StateFlow<Float> = _ownOverallPct.asStateFlow()

    private val _ownMonthlyAttendance = MutableStateFlow<Map<String, Float>>(emptyMap())
    val ownMonthlyAttendance: StateFlow<Map<String, Float>> = _ownMonthlyAttendance.asStateFlow()

    private val _ownRecentDays = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val ownRecentDays: StateFlow<List<Pair<String, String>>> = _ownRecentDays.asStateFlow()

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    fun loadClassesByTeacher(teacherId: String) {
        viewModelScope.launch {
            classRepository.getClassesByTeacher(teacherId).onSuccess { ownClasses ->
                // Merge: keep any extra classes already loaded (from courses in other classes)
                val existing = _classes.value
                val existingIds = ownClasses.map { it.classId }.toSet()
                val extras = existing.filter { it.classId !in existingIds }
                _classes.value = ownClasses + extras
            }
        }
    }

    fun loadStudentsByClass(classId: String) {
        viewModelScope.launch {
            userRepository.getStudentsByClass(classId).onSuccess { _students.value = it }
        }
    }

    fun loadCoursesByTeacher(teacherId: String) {
        viewModelScope.launch {
            // Step 1: load teacher's own assigned classes
            val ownClasses = classRepository.getClassesByTeacher(teacherId).getOrElse { emptyList() }

            // Step 2: load courses by teacherId (admin-assigned courses)
            val coursesByTeacherId = courseRepository.getCoursesByTeacher(teacherId).getOrElse { emptyList() }

            // Step 3: load courses from ALL classes the teacher is assigned to
            val coursesByClass = mutableListOf<Course>()
            ownClasses.forEach { cls ->
                courseRepository.getCoursesByClass(cls.classId).onSuccess { coursesByClass.addAll(it) }
            }

            // Step 4: merge all courses, remove duplicates
            val allCourses = (coursesByTeacherId + coursesByClass).distinctBy { it.courseId }
            _courses.value = allCourses

            // Step 5: find classIds in courses that are NOT in teacher's own classes
            val ownClassIds = ownClasses.map { it.classId }.toSet()
            val missingClassIds = allCourses.map { it.classId }
                .distinct()
                .filter { it.isNotEmpty() && it !in ownClassIds }

            // Step 6: fetch missing classes and build full class list
            val extraClasses = missingClassIds.mapNotNull { classId ->
                classRepository.getClassById(classId).getOrNull()
            }
            _classes.value = ownClasses + extraClasses
        }
    }

    fun loadCoursesByClass(classId: String) {
        viewModelScope.launch {
            courseRepository.getCoursesByClass(classId).onSuccess { 
                val currentCourses = _courses.value.toMutableList()
                currentCourses.addAll(it)
                _courses.value = currentCourses.distinctBy { course -> course.courseId }
            }
        }
    }

    fun loadMaterialsByCourse(courseId: String) {
        viewModelScope.launch {
            materialRepository.getMaterialsByCourse(courseId).onSuccess { _materials.value = it }
        }
    }

    fun loadAttendanceByClass(classId: String) {
        viewModelScope.launch {
            attendanceRepository.getAttendanceByClass(classId).onSuccess { _attendanceRecords.value = it }
        }
    }

    fun addStudent(name: String, email: String, password: String, phone: String, classId: String, organizationId: String = "") {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val result = authRepository.registerStudent(name, email, password, phone, classId, organizationId)
            _operationState.value = result.fold(
                onSuccess = {
                    loadStudentsByClass(classId)
                    OperationState.Success("Student added successfully")
                },
                onFailure = { OperationState.Error(it.message ?: "Failed to add student") }
            )
        }
    }

    fun deleteStudent(uid: String, classId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val result = userRepository.deleteUser(uid)
            _operationState.value = result.fold(
                onSuccess = {
                    loadStudentsByClass(classId)
                    OperationState.Success("Student removed")
                },
                onFailure = { OperationState.Error(it.message ?: "Failed to remove student") }
            )
        }
    }

    fun addCourse(name: String, description: String, classId: String, teacherId: String, teacherName: String, organizationId: String = "") {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val courseId = FirebaseDatabase.getInstance().reference.child("courses").push().key ?: return@launch
            val course = Course(
                courseId = courseId,
                name = name,
                description = description,
                classId = classId,
                teacherId = teacherId,
                teacherName = teacherName,
                organizationId = organizationId
            )
            val result = courseRepository.addCourse(course)
            _operationState.value = result.fold(
                onSuccess = {
                    loadCoursesByTeacher(teacherId)
                    OperationState.Success("Course added successfully")
                },
                onFailure = { OperationState.Error(it.message ?: "Failed to add course") }
            )
        }
    }

    fun deleteCourse(courseId: String, teacherId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val result = courseRepository.deleteCourse(courseId)
            _operationState.value = result.fold(
                onSuccess = {
                    loadCoursesByTeacher(teacherId)
                    OperationState.Success("Course deleted")
                },
                onFailure = { OperationState.Error(it.message ?: "Failed to delete course") }
            )
        }
    }

    fun addStudyMaterial(title: String, description: String, fileUrl: String, fileType: String, courseId: String, uploadedBy: String, uploaderName: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val materialId = FirebaseDatabase.getInstance().reference.child("materials").push().key ?: return@launch
            val material = StudyMaterial(
                materialId = materialId,
                title = title,
                description = description,
                fileUrl = fileUrl,
                fileType = fileType,
                courseId = courseId,
                uploadedBy = uploadedBy,
                uploaderName = uploaderName
            )
            val result = materialRepository.addMaterial(material)
            _operationState.value = result.fold(
                onSuccess = {
                    loadMaterialsByCourse(courseId)
                    OperationState.Success("Material added successfully")
                },
                onFailure = { OperationState.Error(it.message ?: "Failed to add material") }
            )
        }
    }

    fun deleteMaterial(materialId: String, courseId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val result = materialRepository.deleteMaterial(materialId)
            _operationState.value = result.fold(
                onSuccess = {
                    loadMaterialsByCourse(courseId)
                    OperationState.Success("Material deleted")
                },
                onFailure = { OperationState.Error(it.message ?: "Failed to delete material") }
            )
        }
    }

    fun markBulkAttendance(records: List<AttendanceRecord>, classId: String, courseId: String = "") {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val result = attendanceRepository.markBulkAttendance(records)
            _operationState.value = result.fold(
                onSuccess = {
                    val date = records.firstOrNull()?.date
                    if (courseId.isNotEmpty()) {
                        // Course mode: refresh course summary + today's course records
                        loadCourseAttendanceSummary(courseId)
                        if (date != null) loadTodayAttendance(classId, date, courseId)
                    } else {
                        // Class mode: refresh class summary + today's class records
                        loadClassAttendanceSummary(classId)
                        if (date != null) loadTodayAttendance(classId, date, "")
                    }
                    OperationState.Success("Attendance marked successfully")
                },
                onFailure = { OperationState.Error(it.message ?: "Failed to mark attendance") }
            )
        }
    }

    fun loadTodayAttendance(classId: String, date: String, courseId: String = "") {
        viewModelScope.launch {
            _todayAttendanceLoaded.value = false
            _todayAttendanceRecords.value = emptyList() // clear stale data before fetch
            val result = if (courseId.isNotEmpty())
                attendanceRepository.getAttendanceByCourseAndDate(classId, courseId, date)
            else
                attendanceRepository.getAttendanceByClassAndDate(classId, date)
            result.onSuccess { records ->
                _todayAttendanceRecords.value = records
            }.onFailure {
                _todayAttendanceRecords.value = emptyList()
            }
            _todayAttendanceLoaded.value = true
        }
    }

    fun loadClassAttendanceSummary(classId: String) {
        viewModelScope.launch {
            attendanceRepository.getAttendanceByClass(classId).onSuccess { records ->
                _attendanceRecords.value = records
                // Only count class-level (day) records — not course-specific ones
                val classOnly = attendanceRepository.classOnlyRecords(records)
                val byStudent = classOnly.groupBy { it.studentId }
                _classAttendanceSummary.value = byStudent.mapValues { (_, recs) ->
                    attendanceRepository.calculateAttendancePercentage(recs)
                }
            }
        }
    }

    fun loadCourseAttendanceSummary(courseId: String) {
        viewModelScope.launch {
            _courseAttendanceSummary.value = emptyMap()
            attendanceRepository.getAttendanceByCourse(courseId).onSuccess { records ->
                val byStudent = records.groupBy { it.studentId }
                _courseAttendanceSummary.value = byStudent.mapValues { (_, recs) ->
                    attendanceRepository.calculateAttendancePercentage(recs)
                }
            }
        }
    }

    fun loadSelectedStudentAttendance(studentId: String) {
        viewModelScope.launch {
            attendanceRepository.getAttendanceByStudent(studentId).onSuccess { records ->
                _selectedStudentAttendance.value = records
                _selectedStudentOverallPct.value = attendanceRepository.calculateAttendancePercentage(records)
                _selectedStudentMonthly.value = attendanceRepository.calculateMonthlyAttendance(records)
                _selectedStudentRecentDays.value = attendanceRepository.getRecentDaysAttendance(records, 10)
            }
        }
    }

    fun clearSelectedStudentAttendance() {
        _selectedStudentAttendance.value = emptyList()
        _selectedStudentOverallPct.value = 0f
        _selectedStudentMonthly.value = emptyMap()
        _selectedStudentRecentDays.value = emptyList()
    }

    fun loadOwnAttendance(teacherId: String) {
        viewModelScope.launch {
            attendanceRepository.getTeacherAttendance(teacherId).onSuccess { records ->
                _ownAttendanceRecords.value = records
                _ownOverallPct.value = attendanceRepository.calculateTeacherOverallPct(records)
                _ownMonthlyAttendance.value = attendanceRepository.calculateTeacherMonthlyAttendance(records)
                _ownRecentDays.value = attendanceRepository.getTeacherRecentDays(records, 10)
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}
