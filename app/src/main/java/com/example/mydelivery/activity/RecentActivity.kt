package com.example.mydelivery.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mydelivery.adapter.RecentListAdapter
import com.example.mydelivery.databinding.ActivityRecentBinding
import com.example.mydelivery.room.MyRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CoroutineScope(Dispatchers.IO).launch {
            val recent = MyRoomDatabase.getInstance(this@RecentActivity).getRecentDAO()
                .selectRecent()
            withContext(Dispatchers.Main) {
                binding.apply {
                    rvRecent.layoutManager = LinearLayoutManager(this@RecentActivity)
                    rvRecent.adapter = RecentListAdapter(recent)
                }
            }
        }

//        Toast.makeText(this, "Not implement yet", Toast.LENGTH_SHORT).show()
//        Handler(Looper.getMainLooper()).postDelayed(Runnable { onBackPressed() }, 1500)
    }
}