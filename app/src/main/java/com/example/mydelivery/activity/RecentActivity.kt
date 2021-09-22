package com.example.mydelivery.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.mydelivery.R
import com.example.mydelivery.databinding.ActivityRecentBinding

class RecentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Toast.makeText(this, "Not implement yet", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed(Runnable { onBackPressed() }, 1500)
    }
}