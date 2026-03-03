package com.example.studiora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studiora.model.AttendanceRecord
import com.example.studiora.model.Course
import com.example.studiora.model.ScheduleItem
import com.example.studiora.model.StudyMaterial
import com.example.studiora.model.User
import com.example.studiora.repository.AttendanceRepository
import com.example.studiora.repository.AuthRepository
import com.example.studiora.repository.ClassRepository
import com.example.studiora.repository.CourseRepository
import com.example.studiora.repository.StudyMaterialRepository
import com.example.studiora.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val message: String = "") : OperationState()
    data class Error(val message: String) : OperationState()
}

class AdminViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val classRepository = ClassRepository()
    private val courseRepository = CourseRepository()
    private val materialRepository = StudyMaterialRepository()
    private val attendanceRepository = AttendanceRepository()

    /** The uid of the currently signed-in admin — used to scope ALL queries */
    private val organizationId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _selectedUserAttendance = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val selectedUserAttendance: StateFlow<List<AttendanceRecord>> = _selectedUserAttendance.asStateFlow()
    private val _selectedUserMonthlyAttendance = MutableStateFlow<Map<String, Float>>(emptyMap())
    val selectedUserMonthlyAttendance: StateFlow<Map<String, Float>> = _selectedUserMonthlyAttendance.asStateFlow()
    private val _selectedUserRecentDays = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val selectedUserRecentDays: StateFlow<List<Pair<String, String>>> = _selectedUserRecentDays.asStateFlow()
    private val _selectedUserOverallPct = MutableStateFlow(0f)
    val selectedUserOverallPct: StateFlow<Float> = _selectedUserOverallPct.asStateFlow()
    private val _teacherClassAttendance = MutableStateFlow<Map<String, Float>>(emptyMap())
    val teacherClassAttendance: StateFlow<Map<String, Float>> = _teacherClassAttendance.asStateFlow()
    private val _teacherOwnAttendance = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val teacherOwnAttendance: StateFlow<List<Pair<String, String>>> = _teacherOwnAttendance.asStateFlow()
    private val _teacherOwnOverallPct = MutableStateFlow(0f)
    val teacherOwnOverallPct: StateFlow<Float> = _teacherOwnOverallPct.asStateFlow()
    private val _teacherOwnMonthly = MutableStateFlow<Map<String, Float>>(emptyMap())
    val teacherOwnMonthly: StateFlow<Map<String, Float>> = _teacherOwnMonthly.asStateFlow()
    private val _teacherOwnRecentDays = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val teacherOwnRecentDays: StateFlow<List<Pair<String, String>>> = _teacherOwnRecentDays.asStateFlow()
    private val _allTeachersAttendancePct = MutableStateFlow<Map<String, Float>>(emptyMap())
    val allTeachersAttendancePct: StateFlow<Map<String, Float>> = _allTeachersAttendancePct.asStateFlow()
    private val _teachers = MutableStateFlow<List<User>>(emptyList())
    val teachers: StateFlow<List<User>> = _teachers.asStateFlow()
    private val _students = MutableStateFlow<List<User>>(emptyList())
    val students: StateFlow<List<User>> = _students.asStateFlow()
    private val _classes = MutableStateFlow<List<com.example.studiora.model.Class>>(emptyList())
    val classes: StateFlow<List<com.example.studiora.model.Class>> = _classes.asStateFlow()
    private val _selectedClass = MutableStateFlow<com.example.studiora.model.Class?>(null)
    val selectedClass: StateFlow<com.example.studiora.model.Class?> = _selectedClass.asStateFlow()
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()
    private val _courseCount = MutableStateFlow(0)
    val courseCount: StateFlow<Int> = _courseCount.asStateFlow()
    private val _materials = MutableStateFlow<List<StudyMaterial>>(emptyList())
    val materials: StateFlow<List<StudyMaterial>> = _materials.asStateFlow()
    private val _studentCountByClass = MutableStateFlow<Map<String, Int>>(emptyMap())
    val studentCountByClass: StateFlow<Map<String, Int>> = _studentCountByClass.asStateFlow()
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    fun loadAllDashboardData() {
        loadTeachers(); loadStudents(); loadClasses()
        loadCourses(); loadCourseCount(); loadAllTeachersAttendancePct()
    }

    fun loadTeachers() {
        viewModelScope.launch {
            userRepository.getAllTeachers(organizationId).onSuccess { _teachers.value = it }
        }
    }

    fun loadStudents() {
        viewModelScope.launch {
            userRepository.getAllStudents(organizationId).onSuccess { students ->
                _students.value = students
                _studentCountByClass.value = students.groupBy { it.classId }.mapValues { it.value.size }
            }
        }
    }

    fun loadClasses() {
        viewModelScope.launch {
            classRepository.getAllClasses(organizationId).onSuccess { _classes.value = it }
        }
    }

    fun loadClassDetails(classId: String) {
        viewModelScope.launch { classRepository.getClassById(classId).onSuccess { _selectedClass.value = it } }
    }

    fun loadCourses() {
        viewModelScope.launch {
            courseRepository.getAllCourses(organizationId).onSuccess { _courses.value = it }
        }
    }

    fun loadCourseCount() {
        viewModelScope.launch {
            courseRepository.getAllCourses(organizationId).onSuccess { _courseCount.value = it.size }
        }
    }

    fun addTeacher(name: String, email: String, password: String, phone: String, subject: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val r = authRepository.registerTeacher(name, email, password, phone, subject, organizationId)
            _operationState.value = r.fold(
                onSuccess = { loadTeachers(); OperationState.Success("Teacher added successfully") },
                onFailure = { OperationState.Error(it.message ?: "Failed to add teacher") }
            )
        }
    }

    fun addStudent(name: String, email: String, password: String, phone: String, classId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val r = authRepository.registerStudent(name, email, password, phone, classId, organizationId)
            _operationState.value = r.fold(
                onSuccess = { loadStudents(); OperationState.Success("Student added successfully") },
                onFailure = { OperationState.Error(it.message ?: "Failed to add student") }
            )
        }
    }

    fun deleteTeacher(uid: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val r = userRepository.deleteUser(uid)
            _operationState.value = r.fold(
                onSuccess = { loadTeachers(); OperationState.Success("Teacher removed successfully") },
                onFailure = { OperationState.Error(it.message ?: "Failed to remove teacher") }
            )
        }
    }

    fun deleteStudent(uid: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val r = userRepository.deleteUser(uid)
            _operationState.value = r.fold(
                onSuccess = { loadStudents(); OperationState.Success("Student removed successfully") },
                onFailure = { OperationState.Error(it.message ?: "Failed to remove student") }
            )
        }
    }

    fun createClass(name: String, teacherId: String, teacherName: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val classId = com.google.firebase.database.FirebaseDatabase.getInstance()
                .reference.child("classes").push().key ?: return@launch
            val cls = com.example.studiora.model.Class(
                classId = classId, name = name,
                teacherId = teacherId, teacherName = teacherName,
                organizationId = organizationId
            )
            val r = classRepository.createClass(cls)
            _operationState.value = r.fold(
                onSuccess = { loadClasses(); OperationState.Success("Class created successfully") },
                onFailure = { OperationState.Error(it.message ?: "Failed to create class") }
            )
        }
    }

    fun updateClassTeacher(classId: String, newTeacherId: String, newTeacherName: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val r = classRepository.updateClassTeacher(classId, newTeacherId, newTeacherName)
            _operationState.value = r.fold(
                onSuccess = { loadClasses(); OperationState.Success("Teacher updated successfully") },
                onFailure = { OperationState.Error(it.message ?: "Failed to update teacher") }
            )
        }
    }

    fun updateClassSchedule(classId: String, schedule: List<ScheduleItem>) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val r = classRepository.updateClassSchedule(classId, schedule)
            _operationState.value = r.fold(
                onSuccess = { OperationState.Success("Schedule updated successfully") },
                onFailure = { OperationState.Error(it.message ?: "Failed to update schedule") }
            )
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val r = classRepository.deleteClass(classId)
            _operationState.value = r.fold(
                onSuccess = { loadClasses(); OperationState.Success("Class deleted successfully") },
                onFailure = { OperationState.Error(it.message ?: "Failed to delete class") }
            )
        }
    }

    fun loadStudentAttendance(studentId: String) {
        viewModelScope.launch {
            attendanceRepository.getAttendanceByStudent(studentId).onSuccess { allRecords ->
                _selectedUserAttendance.value = allRecords
                // Only class-level (day) records for overall stats — exclude course-specific
                val classOnly = attendanceRepository.classOnlyRecords(allRecords)
                _selectedUserOverallPct.value = attendanceRepository.calculateAttendancePercentage(classOnly)
                _selectedUserMonthlyAttendance.value = attendanceRepository.calculateMonthlyAttendance(classOnly)
                _selectedUserRecentDays.value = attendanceRepository.getRecentDaysAttendance(classOnly, 10)
            }
        }
    }

    fun loadTeacherClassAttendance(teacherClassId: String) {
        viewModelScope.launch {
            attendanceRepository.getAttendanceByClass(teacherClassId).onSuccess { records ->
                // Only class-level records for teacher class summary
                val classOnly = attendanceRepository.classOnlyRecords(records)
                val byStudent = classOnly.groupBy { it.studentId }
                _teacherClassAttendance.value = byStudent.mapValues { (_, recs) ->
                    attendanceRepository.calculateAttendancePercentage(recs)
                }
            }
        }
    }

    fun markTeacherAttendance(teacherId: String, date: String, status: String) {
        viewModelScope.launch {
            val result = attendanceRepository.markTeacherAttendance(teacherId, date, status)
            result.fold(
                onSuccess = {
                    loadTeacherOwnAttendance(teacherId)
                    loadAllTeachersAttendancePct()
                    _operationState.value = OperationState.Success("Attendance marked: $status")
                },
                onFailure = {
                    val msg = if (it.message?.contains("Permission denied", ignoreCase = true) == true)
                        "Permission denied — please update Firebase Database Rules"
                    else it.message ?: "Failed to mark attendance"
                    _operationState.value = OperationState.Error(msg)
                }
            )
        }
    }

    fun loadTeacherOwnAttendance(teacherId: String) {
        viewModelScope.launch {
            attendanceRepository.getTeacherAttendance(teacherId).onSuccess { records ->
                _teacherOwnAttendance.value = records
                _teacherOwnOverallPct.value = attendanceRepository.calculateTeacherOverallPct(records)
                _teacherOwnMonthly.value = attendanceRepository.calculateTeacherMonthlyAttendance(records)
                _teacherOwnRecentDays.value = attendanceRepository.getTeacherRecentDays(records, 10)
            }
        }
    }

    fun loadAllTeachersAttendancePct() {
        viewModelScope.launch {
            attendanceRepository.getAllTeacherAttendance().onSuccess { map ->
                // Filter to only this org's teachers
                val orgTeacherIds = _teachers.value.map { it.uid }.toSet()
                _allTeachersAttendancePct.value = map
                    .filter { (tid, _) -> tid in orgTeacherIds }
                    .mapValues { (_, records) ->
                        attendanceRepository.calculateTeacherOverallPct(records)
                    }
            }
        }
    }

    fun clearSelectedUserAttendance() {
        _selectedUserAttendance.value = emptyList()
        _selectedUserOverallPct.value = 0f
        _selectedUserMonthlyAttendance.value = emptyMap()
        _selectedUserRecentDays.value = emptyList()
        _teacherClassAttendance.value = emptyMap()
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }

    // ── Course Material Management ─────────────────────────────────────────

    fun loadMaterialsByCourse(courseId: String) {
        viewModelScope.launch {
            materialRepository.getMaterialsByCourse(courseId).onSuccess { list ->
                _materials.value = list
            }
        }
    }

    fun addStudyMaterial(
        title: String,
        description: String,
        fileUrl: String,
        fileType: String,
        courseId: String,
        uploadedBy: String,
        uploaderName: String
    ) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val materialId = com.google.firebase.database.FirebaseDatabase.getInstance()
                .reference.child("materials").push().key ?: return@launch
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
            if (result.isSuccess) {
                loadMaterialsByCourse(courseId)
                _operationState.value = OperationState.Success("Material uploaded successfully")
            } else {
                _operationState.value = OperationState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to upload material"
                )
            }
        }
    }

    fun deleteMaterial(materialId: String, courseId: String) {
        viewModelScope.launch {
            _operationState.value = OperationState.Loading
            val result = materialRepository.deleteMaterial(materialId)
            if (result.isSuccess) {
                loadMaterialsByCourse(courseId)
                _operationState.value = OperationState.Success("Material deleted")
            } else {
                _operationState.value = OperationState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to delete material"
                )
            }
        }
    }

    /** Force-refresh a single user from Firebase and update the in-memory teacher/student list */
    fun refreshUserById(uid: String) {
        viewModelScope.launch {
            userRepository.getUserById(uid).onSuccess { freshUser ->
                // Update in teachers list
                val updatedTeachers = _teachers.value.map { if (it.uid == uid) freshUser else it }
                if (updatedTeachers != _teachers.value) {
                    _teachers.value = updatedTeachers
                } else {
                    // Not in teachers — check students
                    val updatedStudents = _students.value.map { if (it.uid == uid) freshUser else it }
                    _students.value = updatedStudents
                }
            }
        }
    }
}

