package com.example.mydelivery.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mydelivery.R
import com.example.mydelivery.activity.MainActivity
import com.example.mydelivery.room.RecentEntity

class RecentListAdapter(private val recentList: List<RecentEntity>) : RecyclerView.Adapter<RecentListAdapter.RecentListHolder>() {
//    private lateinit var context: Context
    class RecentListHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCompany = view.findViewById<TextView>(R.id.tv_recent_company)
        val tvNumber = view.findViewById<TextView>(R.id.tv_recent_number)
        val llRecentLayout: LinearLayout = view.findViewById(R.id.ll_recent_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recent_item, parent, false)
        return RecentListHolder(view)
    }

    override fun onBindViewHolder(holder: RecentListHolder, position: Int) {
        holder.apply {
            tvCompany.text = getCompanyName(recentList[position].company)
            tvNumber.text = recentList[position].trackNumber
            llRecentLayout.setOnClickListener {
                val intent = Intent(itemView.context, MainActivity::class.java).apply {
                    putExtra("recentEntity", recentList[position])
                }
                itemView.context.startActivity(intent)
            }
//            MyLogger.e("size is ${itemCount}, list size is ${recentList.size}")
        }
    }

    override fun getItemCount(): Int {
        return recentList.size
    }

    private fun getCompanyName(name: String) : String = name.replace("kr.", "")
}