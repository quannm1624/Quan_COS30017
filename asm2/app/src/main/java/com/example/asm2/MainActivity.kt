package com.example.asm2

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // List of instruments available for rent, each with name, price, image, and description
    private val instruments = listOf(
        Instrument(
            name = "Euphonia Mixer",
            price = 470,
            imageRes = R.drawable.mixer,
            description = "A professional-grade DJ mixer with advanced audio controls, perfect for seamless transitions and live performances."
        ),
        Instrument(
            name = "DJ DJS-1000 Standalone Sampler",
            price = 450,
            imageRes = R.drawable.sampler,
            description = "A standalone sampler designed for DJs and producers, offering intuitive pad controls and real-time sampling capabilities."
        ),
        Instrument(
            name = "Omnis-Duo Portable Controller",
            price = 550,
            imageRes = R.drawable.allinone,
            description = "A compact and portable DJ controller with built-in audio interfaces, ideal for on-the-go performances and studio sessions."
        ),
        Instrument(
            name = "Opus Quad Controller",
            price = 900,
            imageRes = R.drawable.standard,
            description = "A high-performance DJ controller featuring a quad-core processor, designed for smooth mixing and creative effects."
        ),
        Instrument(
            name = "DJM-S5 Mixer",
            price = 500,
            imageRes = R.drawable.mixer1,
            description = "A versatile DJ mixer with customizable faders and effects, tailored for professional DJs and live performances."
        ),
        Instrument(
            name = "CDJ-3000 Player",
            price = 300,
            imageRes = R.drawable.player,
            description = "A flagship media player with a high-resolution touchscreen, offering advanced playback features for modern DJ setups."
        )
    )

    private var currentInstrumentIndex = 0 // Index to track which instrument is displayed
    private var userCredit = 999999999 // User's available credit
    private val rentedItems = mutableSetOf<String>() // Stores rented instrument names

    // UI components
    private lateinit var greetingMessage: TextView
    private lateinit var instrumentImage: ImageView
    private lateinit var instrumentName: TextView
    private lateinit var instrumentDescription: TextView
    private lateinit var instrumentPrice: TextView
    private lateinit var rentStatusText: TextView
    private lateinit var creditText: TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        greetingMessage = findViewById(R.id.greeting_message)
        instrumentImage = findViewById(R.id.instrument_image)
        instrumentName = findViewById(R.id.instrument_name)
        instrumentDescription = findViewById(R.id.instrument_description)
        instrumentPrice = findViewById(R.id.instrument_price)
        rentStatusText = findViewById(R.id.rent_status)
        creditText = findViewById(R.id.user_credit)

        // Set greeting message
        greetingMessage.text = "Welcome to Miqui's Studio!"

        // Display the first instrument in the list
        updateInstrumentDisplay()

        // Previous button click listener - navigate to the previous instrument
        findViewById<Button>(R.id.prev_button).setOnClickListener {
            if (currentInstrumentIndex > 0) {
                currentInstrumentIndex--
            } else {
                // If at the first item, loop back to the last item
                currentInstrumentIndex = instruments.size - 1
            }
            updateInstrumentDisplay()
        }

        // Next button click listener - navigate to the next instrument
        findViewById<Button>(R.id.next_button).setOnClickListener {
            if (currentInstrumentIndex < instruments.size - 1) {
                currentInstrumentIndex++
            } else {
                // If at the last item, loop back to the first item
                currentInstrumentIndex = 0
            }
            updateInstrumentDisplay()
        }

        // Rent button click listener - open DetailActivity to proceed with rental
        findViewById<Button>(R.id.rent_button).setOnClickListener {
            val currentInstrument = instruments[currentInstrumentIndex]

            if (rentedItems.contains(currentInstrument.name)) {
                // Show message if instrument is already rented
                Toast.makeText(this, "${currentInstrument.name} is already rented!", Toast.LENGTH_SHORT).show()
            } else {
                // Start DetailActivity with instrument details
                val intent = Intent(this, DetailActivity::class.java).apply {
                    putExtra("instrument_name", currentInstrument.name)
                    putExtra("instrument_price", currentInstrument.price)
                    putExtra("instrument_image", currentInstrument.imageRes)
                    putExtra("user_credit", userCredit) // Pass user credit to DetailActivity
                }
                startActivityForResult(intent, 100) // Start activity expecting a result
            }
        }
    }

    // Update the UI to display the details of the currently selected instrument
    private fun updateInstrumentDisplay() {
        val currentInstrument = instruments[currentInstrumentIndex]
        instrumentImage.setImageResource(currentInstrument.imageRes) // Set instrument image
        instrumentName.text = currentInstrument.name // Set instrument name
        instrumentDescription.text = currentInstrument.description // Set instrument description
        instrumentPrice.text = "Price per month: $${currentInstrument.price}" // Set price per month

        // Update rental status text and color
        if (rentedItems.contains(currentInstrument.name)) {
            rentStatusText.text = "Status: Rented"
            rentStatusText.setTextColor(Color.RED) // Set text color to red if rented
        } else {
            rentStatusText.text = "Status: Available"
            rentStatusText.setTextColor(Color.BLUE) // Set text color to blue if available
        }

        creditText.text = ": $$userCredit" // Display the user's remaining credit
    }

    // Handle the result from DetailActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
            val rentedInstrument = data?.getStringExtra("rented_instrument") // Get rented instrument name
            val rentedCost = data?.getIntExtra("rented_cost", 0) ?: 0 // Get rental cost

            rentedInstrument?.let {
                rentedItems.add(it) // Mark instrument as rented
                userCredit -= rentedCost // Deduct rental cost from user credit
                updateInstrumentDisplay() // Update UI with new status
            }
        }
    }
}
