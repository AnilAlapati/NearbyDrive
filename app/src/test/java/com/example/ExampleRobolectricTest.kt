package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun test_main_activity_flow() {
    // Navigate around tabs to see if any screen crashes
    composeTestRule.onNodeWithTag("nav_tab_profile").performClick()
    composeTestRule.onNodeWithTag("nav_tab_host").performClick()
    composeTestRule.onNodeWithTag("nav_tab_bookings").performClick()
    composeTestRule.onNodeWithTag("nav_tab_browse").performClick()
  }

  @Test
  fun test_booking_dialog_interaction() {
    // Wait for composition and database load
    composeTestRule.waitForIdle()

    // Robust wait until vehicles database has fully loaded and populated the Browse screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag("book_button_1").fetchSemanticsNodes().isNotEmpty()
    }

    // Print the root to see what warning cards or elements are present
    composeTestRule.onRoot().printToLog("TEST_LOG")

    // Browse screen is primary. Click "Rent Now" on first vehicle (Tesla Model 3, ID 1)
    composeTestRule.onNodeWithTag("book_button_1").performClick()

    composeTestRule.waitForIdle()
    composeTestRule.onRoot().printToLog("TEST_LOG_AFTER_CLICK")
  }
}
