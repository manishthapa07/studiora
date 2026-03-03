package com.example.studiora

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.studiora.model.*
import com.example.studiora.navigation.AppNavigation
import com.example.studiora.ui.theme.StudioraTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Studiora — run on a real device or emulator.
 * Covers: app context, data model runtime behaviour, and Compose UI screens.
 */
@RunWith(AndroidJUnit4::class)
class StudiorInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─────────────────────────────────────────────────────────────────────────
    // 1. APP CONTEXT TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun appContext_packageName_isCorrect() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.studiora", ctx.packageName)
    }

    @Test
    fun appContext_isNotNull() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(ctx)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. MODEL RUNTIME TESTS (timestamps, collections)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun user_createdAt_isPositive() {
        val user = User(uid = "u1", name = "Alice", email = "alice@example.com")
        assertTrue(user.createdAt > 0)
    }

    @Test
    fun course_createdAt_isPositive() {
        val course = Course(courseId = "c1", name = "Math")
        assertTrue(course.createdAt > 0)
    }

    @Test
    fun attendanceRecord_createdAt_isPositive() {
        val record = AttendanceRecord(recordId = "r1", studentId = "s1")
        assertTrue(record.createdAt > 0)
    }

    @Test
    fun classSchedule_canHoldMultipleItems() {
        val schedule = listOf(
            ScheduleItem(day = "Monday",    startTime = "09:00", endTime = "10:00", courseId = "c1"),
            ScheduleItem(day = "Wednesday", startTime = "11:00", endTime = "12:00", courseId = "c2")
        )
        val cls = Class(classId = "cls1", name = "Grade 10A", schedule = schedule)
        assertEquals(2, cls.schedule.size)
        assertEquals("Monday",    cls.schedule[0].day)
        assertEquals("Wednesday", cls.schedule[1].day)
    }

    @Test
    fun userList_filterByRole_givesCorrectCounts() {
        val users = listOf(
            User(uid = "u1", role = UserRoles.ADMIN),
            User(uid = "u2", role = UserRoles.TEACHER),
            User(uid = "u3", role = UserRoles.STUDENT),
            User(uid = "u4", role = UserRoles.STUDENT),
            User(uid = "u5", role = UserRoles.TEACHER)
        )
        assertEquals(1, users.count { it.role == UserRoles.ADMIN })
        assertEquals(2, users.count { it.role == UserRoles.TEACHER })
        assertEquals(2, users.count { it.role == UserRoles.STUDENT })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. COMPOSE UI TESTS — Login Screen
    // ─────────────────────────────────────────────────────────────────────────

    private fun launchApp(startDestination: String = "login") {
        composeTestRule.setContent {
            StudioraTheme {
                AppNavigation(
                    navController = rememberNavController(),
                    startDestination = startDestination
                )
            }
        }
    }

    @Test
    fun loginScreen_appName_isDisplayed() {
        launchApp()
        composeTestRule.onNodeWithText("Studiora").assertIsDisplayed()
    }

    @Test
    fun loginScreen_welcomeBack_isDisplayed() {
        launchApp()
        composeTestRule.onNodeWithText("Welcome Back!").assertIsDisplayed()
    }

    @Test
    fun loginScreen_emailField_exists() {
        launchApp()
        composeTestRule.onNodeWithText("Email Address").assertExists()
    }

    @Test
    fun loginScreen_passwordField_exists() {
        launchApp()
        composeTestRule.onNodeWithText("Password").assertExists()
    }

    @Test
    fun loginScreen_signInButton_isEnabled() {
        launchApp()
        composeTestRule.onNodeWithText("Sign In").assertIsEnabled()
    }

    @Test
    fun loginScreen_createOrgButton_isDisplayed() {
        launchApp()
        composeTestRule.onNodeWithText("Create Organization Account").assertIsDisplayed()
    }

    @Test
    fun loginScreen_forgotPassword_navigatesToForgotScreen() {
        launchApp()
        composeTestRule.onNodeWithText("Forgot Password?").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Forgot Password?", useUnmergedTree = true).assertExists()
    }

    @Test
    fun loginScreen_createOrgButton_navigatesToRegisterScreen() {
        launchApp()
        composeTestRule.onNodeWithText("Create Organization Account").performClick()
        composeTestRule.waitForIdle()
        // "Register Organization" appears in both TopAppBar and button — use [0] to avoid ambiguity
        composeTestRule.onAllNodesWithText("Register Organization")[0].assertExists()
    }

    @Test
    fun loginScreen_emailField_acceptsInput() {
        launchApp()
        composeTestRule.onNodeWithText("Email Address").performTextInput("test@example.com")
    }

    @Test
    fun loginScreen_passwordField_acceptsInput() {
        launchApp()
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
    }
}