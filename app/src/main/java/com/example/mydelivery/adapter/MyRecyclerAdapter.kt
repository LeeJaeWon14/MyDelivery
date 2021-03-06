package com.example.mydelivery.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mydelivery.R
import com.example.mydelivery.network.dto.ProgressDTO

class MyRecyclerAdapter(private val progresses : List<ProgressDTO>) : RecyclerView.Adapter<MyRecyclerAdapter.MyRecyclerHolder>() {
    class MyRecyclerHolder(view : View) : RecyclerView.ViewHolder(view) {
        val time : TextView = view.findViewById(R.id.label_time)
        val status : TextView = view.findViewById(R.id.label_status)
        val location : TextView = view.findViewById(R.id.label_location)
        val description : TextView = view.findViewById(R.id.label_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyRecyclerHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
        return MyRecyclerHolder(view)
    }

    override fun getItemCount(): Int {
        return progresses.size
    }

    override fun onBindViewHolder(holder: MyRecyclerHolder, position: Int) {
        holder.time.text = timePrettyPrint(progresses[position].time)
        holder.status.text = progresses[position].status.text
        holder.location.text = progresses[position].location.name
        holder.description.text = progresses[position].description
    }

    private fun timePrettyPrint(time : String) : String = time.replace("T", "\n").split("+")[0]
}