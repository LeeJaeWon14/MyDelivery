package com.example.mydelivery.activity

import android.content.DialogInterface
import android.os.Bundle
import android.transition.Slide
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mydelivery.R
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
        with(window) {
            requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            enterTransition = Slide(Gravity.END)
            exitTransition = Slide(Gravity.START)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityRecentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // UI init
        binding.apply {
            actionBar?.hide()
            setSupportActionBar(binding.tbRecentAct)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            tbRecentAct.title = getString(R.string.str_recent_activity)
            tbRecentAct.textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        CoroutineScope(Dispatchers.IO).launch {
            val recent = MyRoomDatabase.getInstance(this@RecentActivity).getRecentDAO()
                .selectRecent()
            withContext(Dispatchers.Main) {
                binding.apply {
                    rvRecent.layoutManager = LinearLayoutManager(this@RecentActivity)
                    rvRecent.adapter = RecentListAdapter(recent.toMutableList())
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.recent_toolbat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_remove_all -> {
                AlertDialog.Builder(this)
                    .setMessage("모두 삭제됩니다")
                    .setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
                        CoroutineScope(Dispatchers.IO).launch {
                            MyRoomDatabase.getInstance(this@RecentActivity).getRecentDAO()
                                .removeAll()
                        }
                        binding.rvRecent.adapter?.notifyDataSetChanged()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
            android.R.id.home -> {
                onBackPressed()
            }
        }
        return true
    }
}