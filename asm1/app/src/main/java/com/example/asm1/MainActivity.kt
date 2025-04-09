package com.example.asm1

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    // Track the current score and position
    private var currentScore = 0
    private var currentHold = 0
    private var hasFallen = false

    // UI elements
    private lateinit var score: TextView
    private lateinit var climb: Button
    private lateinit var fall: Button
    private lateinit var reset: Button

    //save scoring system when doing rotation
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Starting activity initialization")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()

        if (savedInstanceState != null) {
            Log.i(TAG, "onCreate: Restoring saved instance state")
            restoreState(savedInstanceState)
        } else {
            Log.i(TAG, "onCreate: No saved state found, using default values")
        }

        setupClickListeners()
        updateDisplay()
    }

    private fun initializeViews() {
        Log.v(TAG, "initializeViews: Finding views by ID")
        score = findViewById(R.id.scoreText)
        climb = findViewById(R.id.climbButton)
        fall = findViewById(R.id.fallButton)
        reset = findViewById(R.id.resetButton)

        // Set button text using string resources
        climb.text = getString(R.string.button_climb)
        fall.text = getString(R.string.button_fall)
        reset.text = getString(R.string.button_reset)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        try {
            currentScore = savedInstanceState.getInt(STATE_SCORE, 0)
            currentHold = savedInstanceState.getInt(STATE_HOLD, 0)
            hasFallen = savedInstanceState.getBoolean(STATE_FALLEN, false)
            Log.i(TAG, "restoreState: State restored - Score: $currentScore, Hold: $currentHold, Fallen: $hasFallen")
        } catch (e: Exception) {
            Log.e(TAG, "restoreState: Error restoring state", e)
            resetGameState()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState: Saving current game state")
        super.onSaveInstanceState(outState)

        try {
            outState.apply {
                putInt(STATE_SCORE, currentScore)
                putInt(STATE_HOLD, currentHold)
                putBoolean(STATE_FALLEN, hasFallen)
            }
            Log.i(TAG, "onSaveInstanceState: State saved successfully - Score: $currentScore, Hold: $currentHold, Fallen: $hasFallen")
        } catch (e: Exception) {
            Log.e(TAG, "onSaveInstanceState: Error saving state", e)
        }
    }

    private fun setupClickListeners() {
        Log.v(TAG, "setupClickListeners: Setting up button click listeners")
        //Climb button
        climb.setOnClickListener {
            Log.d(TAG, "Climb button clicked - Current hold: $currentHold")
            if (!hasFallen && currentHold < 9) {
                currentHold++
                Log.i(TAG, "Processing climb - New hold position: $currentHold")
                updateScore()
            } else {
                Log.w(TAG, "Climb attempted but not allowed - Fallen: $hasFallen, Current hold: $currentHold")
            }
        }
        //Fall button
        fall.setOnClickListener {
            Log.d(TAG, "Fall button clicked - Current hold: $currentHold")
            if (currentHold > 0 && currentHold < 9 && !hasFallen) {
                hasFallen = true
                val oldScore = currentScore
                currentScore = maxOf(0, currentScore - 3)
                Log.i(TAG, "Processing fall - Score changed from $oldScore to $currentScore")
                updateDisplay()
            } else {
                Log.w(TAG, "Fall attempted but not allowed - Current hold: $currentHold, Already fallen: $hasFallen")
            }
        }
        //Reset button
        reset.setOnClickListener {
            Log.d(TAG, "Reset button clicked")
            resetGameState()
        }
    }

    private fun resetGameState() {
        Log.i(TAG, "resetGameState: Resetting game state from Score: $currentScore, Hold: $currentHold")
        currentScore = 0
        currentHold = 0
        hasFallen = false
        updateDisplay()
        Log.i(TAG, "resetGameState: Game state reset complete")
    }
    //Scoring
    private fun updateScore() {
        val oldScore = currentScore
        // Calculate points based on the zone
        val points = when (currentHold) {
            in 1..3 -> 1  // Blue zone
            in 4..6 -> 2  // Green zone
            in 7..9 -> 3  // Red zone
            else -> 0
        }

        currentScore += points
        Log.d(TAG, "updateScore: Score updated from $oldScore to $currentScore (added $points points)")
        updateDisplay()
    }
    //Score zones
    private fun updateDisplay() {
        Log.v(TAG, "updateDisplay: Updating UI elements")
        try {
            // Update score text
            score.text =  getString(R.string.score_text, currentScore)
            getString(R.string.score_text, currentScore)
            // Update text color based on current zone
            val zoneColor = when (currentHold) {
                in 1..3 -> Color.BLUE
                in 4..6 -> Color.GREEN
                in 7..9 -> Color.RED
                else -> Color.BLACK
            }
            score.setTextColor(zoneColor)

            // Update button states
            climb.isEnabled = !hasFallen && currentHold < 9
            fall.isEnabled = currentHold > 0 && currentHold < 9 && !hasFallen

            Log.v(TAG, "updateDisplay: Display updated successfully - Zone: ${
                when(currentHold) {
                    in 1..3 -> "Blue"
                    in 4..6 -> "Green"
                    in 7..9 -> "Red"
                    else -> "None"
                }
            }")
        } catch (e: Exception) {
            Log.e(TAG, "updateDisplay: Error updating display", e)
        }
    }

    companion object {
        // Tag for logging
        private const val TAG = "ClimbingScorer"

        // Keys for saving state
        private const val STATE_SCORE = "currentScore"
        private const val STATE_HOLD = "currentHold"
        private const val STATE_FALLEN = "hasFallen"
    }
}