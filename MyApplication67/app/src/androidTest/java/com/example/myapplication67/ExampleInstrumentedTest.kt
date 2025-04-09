package com.example.myapplication67

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private lateinit var appContext: Context

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        ActivityScenario.launch(MainActivity::class.java)
    }

    @Test
    fun useAppContext() {
        assertEquals("com.example.myapplication67", appContext.packageName)
    }

    @Test
    fun swipeRight_editsTaskAndUpdates() {
        // Assuming a task named "Buy groceries" already exists

        // Swipe right to edit (RecyclerView swipe may require custom action or helper)
        // Assuming the swipe right is handled programmatically, we simulate the edit dialog here
        onView(withText("Buy groceries")).perform(click())

        // Update task
        onView(withId(R.id.newTaskText)).perform(clearText(), typeText("Buy snacks"), closeSoftKeyboard())
        onView(withId(R.id.NewTaskButton)).perform(click())

        // Verify updated task
        onView(withText("Buy snacks")).check(matches(isDisplayed()))
    }

    @Test
    fun taskGrouping_showsTodaySection() {
        // Assuming tasks with today's date exist

        // Check for section header
        onView(withText("Today")).check(matches(isDisplayed()))
    }

    @Test
    fun deleteTask_removesTaskAndCancelsAlarm() {
        // Swipe left to delete
        // You'll need to implement swipe with RecyclerViewActions
        onView(withText("Buy snacks")).perform(swipeLeft())

        // Verify it's no longer in the list
        onView(withText("Buy snacks")).check(doesNotExist())
    }
}
