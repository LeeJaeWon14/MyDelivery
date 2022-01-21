package com.example.mydelivery.adapter

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mydelivery.R
import com.example.mydelivery.activity.MainActivity
import com.example.mydelivery.room.MyRoomDatabase
import com.example.mydelivery.room.RecentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecentListAdapter(private val recentList: MutableList<RecentEntity>) : RecyclerView.Adapter<RecentListAdapter.RecentListHolder>() {
//    private lateinit var context: Context
    class RecentListHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCompany: TextView = view.findViewById(R.id.tv_recent_company)
        val tvNumber: TextView = view.findViewById(R.id.tv_recent_number)
        val llRecentLayout: LinearLayout = view.findViewById(R.id.ll_recent_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recent_item, parent, false)
        return RecentListHolder(view)
    }

    override fun onBindViewHolder(holder: RecentListHolder, position: Int) {
        holder.apply {
            tvCompany.text = recentList[position].companyName
            tvNumber.text = recentList[position].trackNumber
            llRecentLayout.setOnClickListener {
                val intent = Intent(itemView.context, MainActivity::class.java).apply {
                    putExtra("recentEntity", recentList[position])
                }
                itemView.context.startActivity(intent)
            }
            llRecentLayout.setOnLongClickListener {
                AlertDialog.Builder(itemView.context)
                    .setMessage(getString(R.string.str_remove_confirm, itemView))
                    .setPositiveButton(getString(R.string.str_remove_label, itemView)) { dialogInterface: DialogInterface, i: Int ->
                        CoroutineScope(Dispatchers.IO).launch {
                            MyRoomDatabase.getInstance(itemView.context).getRecentDAO()
                                .deleteRecent(recentList[position])
                            recentList.removeAt(position)
                        }
                        notifyDataSetChanged()
                    }
                    .setNegativeButton(getString(R.string.str_dialog_cancel_label, itemView), null)
                    .setCancelable(false)
                    .show()
                true
            }
        }
    }

    override fun getItemCount(): Int {
        return recentList.size
    }

//    private fun getCompanyName(name: String) : String = name.replace("kr.", "")
    private fun getString(resId: Int, view: View) : String = RecentListHolder(view).itemView.context.getString(resId)
}