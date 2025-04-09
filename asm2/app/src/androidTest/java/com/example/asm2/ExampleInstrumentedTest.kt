package com.example.asm2

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RentalInstrumentTest {

    @get:Rule
        var activityScenarioRule = ActivityScenarioRule(DetailActivity::class.java) // Change to your activity

    @Test
    fun testRentalInputFields() {
        // Check if Rental Period input is displayed
        onView(withId(R.id.rental_period_input))
            .check(matches(isDisplayed()))

        // Check if Quantity input is displayed
        onView(withId(R.id.quantity_input))
            .check(matches(isDisplayed()))

        // Enter values
        onView(withId(R.id.rental_period_input))
            .perform(clearText(), typeText("3"), closeSoftKeyboard())

        onView(withId(R.id.quantity_input))
            .perform(clearText(), typeText("2"), closeSoftKeyboard())

        // Click the Proceed button
        onView(withId(R.id.proceed_button))
            .perform(click())
    }
}
