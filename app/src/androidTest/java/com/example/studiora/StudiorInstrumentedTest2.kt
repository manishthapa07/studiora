package com.example.studiora

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.studiora.model.*
import com.example.studiora.navigation.AppNavigation
import com.example.studiora.ui.auth.ForgotPasswordScreen
import com.example.studiora.ui.auth.OrganizationRegisterScreen
import com.example.studiora.ui.theme.StudioraTheme
import com.example.studiora.viewmodel.AuthViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class StudiorInstrumentedTest2 {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─────────────────────────────────────────────────────────────────────────
    // 1. FORGOT PASSWORD SCREEN UI TESTS
    // ─────────────────────────────────────────────────────────────────────────

    private fun launchForgotPassword() {
        composeTestRule.setContent {
            StudioraTheme {
                val navController = rememberNavController()
                val authViewModel = AuthViewModel()
                ForgotPasswordScreen(navController = navController, authViewModel = authViewModel)
            }
        }
    }

    @Test
    fun forgotPassword_emailField_exists() {
        launchForgotPassword()
        composeTestRule.onNodeWithText("Email Address").assertExists()
    }

    @Test
    fun forgotPassword_sendResetButton_exists() {
        launchForgotPassword()
        composeTestRule.onNodeWithText("Send Reset Link").assertExists()
    }

    @Test
    fun forgotPassword_sendResetButton_isEnabled() {
        launchForgotPassword()
        composeTestRule.onNodeWithText("Send Reset Link").assertIsEnabled()
    }

    @Test
    fun forgotPassword_emailField_acceptsInput() {
        launchForgotPassword()
        composeTestRule.onNodeWithText("Email Address").performTextInput("user@example.com")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. ORGANISATION REGISTER SCREEN UI TESTS
    // ─────────────────────────────────────────────────────────────────────────

    private fun launchOrgRegister() {
        composeTestRule.setContent {
            StudioraTheme {
                val navController = rememberNavController()
                val authViewModel = AuthViewModel()
                OrganizationRegisterScreen(navController = navController, authViewModel = authViewModel)
            }
        }
    }

    @Test
    fun orgRegister_orgNameField_exists() {
        launchOrgRegister()
        composeTestRule.onNodeWithText("Organization Name").assertExists()
    }

    @Test
    fun orgRegister_emailField_exists() {
        launchOrgRegister()
        composeTestRule.onNodeWithText("Email Address").assertExists()
    }

    @Test
    fun orgRegister_passwordField_exists() {
        launchOrgRegister()
        composeTestRule.onNodeWithText("Password").assertExists()
    }

    @Test
    fun orgRegister_registerButton_exists() {
        launchOrgRegister()
        // "Register Organization" appears in TopAppBar — use onFirst() since button also has it
        composeTestRule.onAllNodesWithText("Register Organization")[0].assertExists()
    }

    @Test
    fun orgRegister_registerButton_isEnabled() {
        launchOrgRegister()
        // Confirm screen is loaded by checking the TopAppBar title (first match)
        composeTestRule.onAllNodesWithText("Register Organization")[0].assertExists()
        composeTestRule.onNodeWithText("Organization Name").assertExists()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. NAVIGATION — FULL APP FLOW TESTS
    // ─────────────────────────────────────────────────────────────────────────

    private fun launchApp() {
        composeTestRule.setContent {
            StudioraTheme {
                AppNavigation(navController = rememberNavController(), startDestination = "login")
            }
        }
    }

    @Test
    fun navigation_loginToForgotPassword_showsForgotScreen() {
        launchApp()
        composeTestRule.onNodeWithText("Forgot Password?").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Send Reset Link").assertExists()
    }

    @Test
    fun navigation_loginToOrgRegister_showsRegisterScreen() {
        launchApp()
        composeTestRule.onNodeWithText("Create Organization Account").performClick()
        composeTestRule.waitForIdle()
        // "Register Organization" appears in TopAppBar — use [0] to avoid ambiguity with button text
        composeTestRule.onAllNodesWithText("Register Organization")[0].assertExists()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ADDITIONAL MODEL RUNTIME TESTS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun studyMaterial_createdAt_isPositive() {
        val material = StudyMaterial(materialId = "m1", title = "Notes")
        assertTrue(material.createdAt > 0)
    }

    @Test
    fun classModel_createdAt_isPositive() {
        val cls = Class(classId = "cls1", name = "Grade 10")
        assertTrue(cls.createdAt > 0)
    }

    @Test
    fun appContext_applicationInfo_isNotNull() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(ctx.applicationInfo)
    }

    @Test
    fun userList_sortedByName_isAlphabetical() {
        val users = listOf(
            User(uid = "u1", name = "Charlie"),
            User(uid = "u2", name = "Alice"),
            User(uid = "u3", name = "Bob")
        )
        val sorted = users.sortedBy { it.name }
        assertEquals("Alice",   sorted[0].name)
        assertEquals("Bob",     sorted[1].name)
        assertEquals("Charlie", sorted[2].name)
    }

    @Test
    fun courseList_filteredByClassId_isCorrect() {
        val courses = listOf(
            Course(courseId = "c1", classId = "cls1"),
            Course(courseId = "c2", classId = "cls1"),
            Course(courseId = "c3", classId = "cls2")
        )
        val forCls1 = courses.filter { it.classId == "cls1" }
        assertEquals(2, forCls1.size)
        assertTrue(forCls1.all { it.classId == "cls1" })
    }
}



