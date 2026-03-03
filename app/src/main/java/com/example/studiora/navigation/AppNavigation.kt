package com.example.studiora.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.studiora.ui.admin.AdminCourseMaterialsScreen
import com.example.studiora.ui.admin.AdminCoursesScreen
import com.example.studiora.ui.admin.AdminDashboardScreen
import com.example.studiora.ui.admin.AdminStudentDetailScreen
import com.example.studiora.ui.admin.AdminTeacherDetailScreen
import com.example.studiora.ui.admin.ClassRoutineScreen
import com.example.studiora.ui.admin.ManageClassRoutineScreen
import com.example.studiora.ui.admin.ManageClassesScreen
import com.example.studiora.ui.admin.ManageClassScheduleScreen
import com.example.studiora.ui.admin.ManageStudentsScreen
import com.example.studiora.ui.admin.ManageTeachersScreen
import com.example.studiora.ui.auth.ForgotPasswordScreen
import com.example.studiora.ui.auth.LoginScreen
import com.example.studiora.ui.auth.OrganizationRegisterScreen
import com.example.studiora.ui.common.ProfileScreen
import com.example.studiora.ui.student.StudentAttendanceScreen
import com.example.studiora.ui.student.StudentCoursesScreen
import com.example.studiora.ui.student.StudentDashboardScreen
import com.example.studiora.ui.student.StudentMaterialsScreen
import com.example.studiora.ui.student.StudentWeeklyRoutineScreen
import com.example.studiora.ui.teacher.AttendanceScreen
import com.example.studiora.ui.teacher.TeacherCoursesScreen
import com.example.studiora.ui.teacher.TeacherDashboardScreen
import com.example.studiora.ui.teacher.TeacherMaterialsScreen
import com.example.studiora.ui.teacher.TeacherStudentsScreen
import com.example.studiora.ui.teacher.TeacherWeeklyRoutineScreen
import com.example.studiora.viewmodel.AdminViewModel
import com.example.studiora.viewmodel.AuthViewModel
import com.example.studiora.viewmodel.StudentViewModel
import com.example.studiora.viewmodel.TeacherViewModel

private val enterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(280))
}
private val exitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutHorizontally(tween(280)) { -it / 4 } + fadeOut(tween(180))
}
private val popEnterTransition: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(280))
}
private val popExitTransition: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutHorizontally(tween(280)) { it / 4 } + fadeOut(tween(180))
}

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String) {
    val authViewModel: AuthViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel()
    val teacherViewModel: TeacherViewModel = viewModel()
    val studentViewModel: StudentViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition
    ) {
        composable("login") {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("forgotPassword") {
            ForgotPasswordScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("profile") {
            ProfileScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("org_register") {
            OrganizationRegisterScreen(navController = navController, authViewModel = authViewModel)
        }
        // Admin routes
        composable("admin_dashboard") {
            AdminDashboardScreen(navController = navController, authViewModel = authViewModel, adminViewModel = adminViewModel)
        }
        composable("manage_teachers") {
            ManageTeachersScreen(navController = navController, adminViewModel = adminViewModel)
        }
        composable("admin_teacher_detail/{teacherId}") { backStackEntry ->
            val teacherId = backStackEntry.arguments?.getString("teacherId") ?: ""
            AdminTeacherDetailScreen(teacherId = teacherId, navController = navController, adminViewModel = adminViewModel)
        }
        composable("manage_students") {
            ManageStudentsScreen(navController = navController, adminViewModel = adminViewModel)
        }
        composable("admin_student_detail/{studentId}") { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            AdminStudentDetailScreen(studentId = studentId, navController = navController, adminViewModel = adminViewModel)
        }
        composable("manage_classes") {
            ManageClassesScreen(navController = navController, adminViewModel = adminViewModel)
        }
        composable("admin_courses") {
            AdminCoursesScreen(navController = navController, adminViewModel = adminViewModel)
        }
        composable("admin_course_materials/{courseId}/{courseName}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
            AdminCourseMaterialsScreen(
                navController = navController,
                courseId = courseId,
                courseName = courseName,
                adminViewModel = adminViewModel,
                authViewModel = authViewModel
            )
        }
        composable("manage_class_routine") {
            ManageClassRoutineScreen(navController = navController, adminViewModel = adminViewModel)
        }
        composable("manage_class_schedule/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ManageClassScheduleScreen(classId = classId, navController = navController, adminViewModel = adminViewModel)
        }
        composable("class_routine") {
            ClassRoutineScreen(navController = navController, adminViewModel = adminViewModel)
        }
        // Teacher routes
        composable("teacher_dashboard") {
            TeacherDashboardScreen(navController = navController, authViewModel = authViewModel, teacherViewModel = teacherViewModel)
        }
        composable("teacher_students/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            TeacherStudentsScreen(navController = navController, classId = classId, teacherViewModel = teacherViewModel, authViewModel = authViewModel)
        }
        composable("teacher_courses") {
            TeacherCoursesScreen(navController = navController, authViewModel = authViewModel, teacherViewModel = teacherViewModel)
        }
        composable("teacher_materials/{courseId}/{courseName}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
            TeacherMaterialsScreen(navController = navController, courseId = courseId, courseName = courseName, authViewModel = authViewModel, teacherViewModel = teacherViewModel)
        }
        composable("attendance/{classId}/{className}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val className = backStackEntry.arguments?.getString("className") ?: ""
            AttendanceScreen(navController = navController, classId = classId, className = className, authViewModel = authViewModel, teacherViewModel = teacherViewModel)
        }
        // Course-specific attendance: attendance/classId/className/courseId/courseName
        composable("course_attendance/{classId}/{className}/{courseId}/{courseName}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            val className = backStackEntry.arguments?.getString("className") ?: ""
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
            AttendanceScreen(
                navController = navController,
                classId = classId,
                className = className,
                authViewModel = authViewModel,
                teacherViewModel = teacherViewModel,
                courseId = courseId,
                courseName = courseName
            )
        }
        composable("teacher_weekly_routine") {
            TeacherWeeklyRoutineScreen(navController = navController, authViewModel = authViewModel, teacherViewModel = teacherViewModel)
        }
        // Student routes
        composable("student_dashboard") {
            StudentDashboardScreen(navController = navController, authViewModel = authViewModel, studentViewModel = studentViewModel)
        }
        composable("student_courses") {
            StudentCoursesScreen(navController = navController, authViewModel = authViewModel, studentViewModel = studentViewModel)
        }
        composable("student_materials/{courseId}/{courseName}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
            StudentMaterialsScreen(navController = navController, courseId = courseId, courseName = courseName, studentViewModel = studentViewModel)
        }
        composable("student_attendance") {
            StudentAttendanceScreen(navController = navController, authViewModel = authViewModel, studentViewModel = studentViewModel)
        }
        composable("student_weekly_routine") {
            StudentWeeklyRoutineScreen(navController = navController, authViewModel = authViewModel, studentViewModel = studentViewModel)
        }
    }
}
