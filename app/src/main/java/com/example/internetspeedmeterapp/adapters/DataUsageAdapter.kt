package com.example.internetspeedmeterapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.internetspeedmeterapp.R
import com.example.internetspeedmeterapp.datahandler.DataUsageHandler

class DataUsageAdapter(private val dataUsageList: List<DataUsageHandler>) :
    RecyclerView.Adapter<DataUsageAdapter.DataUsageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataUsageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_data_usage, parent, false)
        return DataUsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: DataUsageViewHolder, position: Int) {
        val data = dataUsageList[position]
        holder.bind(data)
    }

    override fun getItemCount(): Int = dataUsageList.size

    class DataUsageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeTextView: TextView = itemView.findViewById(R.id.typeTextView)
        private val usageTextView: TextView = itemView.findViewById(R.id.usageTextView)

        fun bind(data: DataUsageHandler) {
            typeTextView.text = data.type
            usageTextView.text = data.usage
        }
    }
}
