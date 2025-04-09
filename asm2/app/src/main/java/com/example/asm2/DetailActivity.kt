package com.example.asm2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class DetailActivity : AppCompatActivity() {

    // UI elements
    private lateinit var instrumentImage: ImageView
    private lateinit var instrumentName: TextView
    private lateinit var rentalPeriodInput: EditText
    private lateinit var quantityInput: EditText
    private lateinit var totalCostText: TextView
    private lateinit var equipmentContainer: LinearLayout

    // Variables to store instrument price, user selection, and credit details
    private var instrumentPrice: Int = 0
    private var selectedDay: Int = 0
    private var selectedQuantity: Int = 0
    private var totalCost: Int = 0
    private var userCredit: Int = 0
    private val selectedEquipment = mutableSetOf<String>() // Set to store selected additional equipment

    // List of available equipment with their prices
    private val equipmentOptions = listOf(
        "Wired Standard Headphone" to 100,
        "Wired Ear-Cup Headphone" to 200,
        "Bluetooth Speaker" to 150,
        "Hi-End Wired Speaker" to 700
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_activity)

        // Initialize UI elements
        instrumentImage = findViewById(R.id.instrument_image2)
        instrumentName = findViewById(R.id.instrument_name_sub)
        rentalPeriodInput = findViewById(R.id.rental_period_input)
        quantityInput = findViewById(R.id.quantity_input)
        totalCostText = findViewById(R.id.total_cost)
        equipmentContainer = findViewById(R.id.equipment_container)

        // Retrieve data from intent (passed from the previous activity)
        instrumentName.text = intent.getStringExtra("instrument_name")
        instrumentPrice = intent.getIntExtra("instrument_price", 0)
        instrumentImage.setImageResource(intent.getIntExtra("instrument_image", 0))
        userCredit = intent.getIntExtra("user_credit", 0)

        // Add text change listeners to rental period and quantity inputs to update the total cost dynamically
        rentalPeriodInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateAndUpdateCost() // Update cost whenever input changes
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        quantityInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateAndUpdateCost() // Update cost whenever input changes
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle "Proceed" button click
        findViewById<Button>(R.id.proceed_button).setOnClickListener {
            if (selectedDay == 0 || selectedQuantity == 0) {
                // Show a message if rental period or quantity is invalid
                Toast.makeText(
                    this,
                    "Please enter valid rental period and quantity!",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (totalCost > userCredit) {
                // Show a message if user credit is insufficient
                Toast.makeText(this, "Insufficient credit!", Toast.LENGTH_SHORT).show()
            } else {
                // Return rental details to the previous activity
                val resultIntent = Intent().apply {
                    putExtra("rented_instrument", instrumentName.text.toString())
                    putExtra("rented_cost", totalCost)
                    putExtra("selected_equipment", selectedEquipment.toTypedArray())
                }
                setResult(RESULT_OK, resultIntent)
                Toast.makeText(this, "Successfully Rented!", Toast.LENGTH_SHORT).show()
                finish() // Close activity
            }
        }

        // Handle "Cancel" button click (simply closes the activity)
        findViewById<Button>(R.id.cancel_button).setOnClickListener { finish() }

        // Add available equipment options to the UI dynamically
        addEquipmentOptions()
    }

    private fun addEquipmentOptions() {
        for ((name, price) in equipmentOptions) {
            val checkBox = CheckBox(this).apply {
                text = "$name (+$$price)"
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedEquipment.add(name) else selectedEquipment.remove(name)
                    updateCost() // Update total cost when equipment selection changes
                }
            }
            equipmentContainer.addView(checkBox) // Add checkbox to the layout
        }
    }

    private fun validateAndUpdateCost() {
        val dayText = rentalPeriodInput.text.toString()
        val quantityText = quantityInput.text.toString()

        // Convert text input to integer values (default to 0 if invalid)
        selectedDay = dayText.toIntOrNull() ?: 0
        selectedQuantity = quantityText.toIntOrNull() ?: 0

        // Update the total cost after input validation
        updateCost()
    }

    private fun updateCost() {
        // Calculate base cost: instrument price * rental period * quantity
        val baseCost = instrumentPrice * selectedDay * selectedQuantity

        // Calculate additional equipment cost: sum of selected equipment prices * quantity
        val equipmentCost = equipmentOptions
            .filter { it.first in selectedEquipment }
            .sumOf { it.second } * selectedQuantity

        // Total cost is base cost + equipment cost
        totalCost = baseCost + equipmentCost
        totalCostText.text = "Total Cost: $$totalCost" // Update UI with new total cost
    }
}
