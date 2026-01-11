package com.nimsaya.calclite.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nimsaya.calclite.R
import com.nimsaya.calclite.activities.DetailActivity
import com.nimsaya.calclite.models.HistoryModel

class HistoryAdapter(val historyList: List<HistoryModel>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var isEditMode = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvExpression: TextView = view.findViewById(R.id.tvExpression)
        val tvResult: TextView = view.findViewById(R.id.tvResult)
        val checkDelete: CheckBox = view.findViewById(R.id.checkDelete)
        // Menghubungkan ikon panah sebagai tombol detail
        val imgDetail: ImageView = view.findViewById(R.id.imgDetailArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]

        holder.tvExpression.text = item.formula
        holder.tvResult.text = item.result

        // Kontrol visibilitas untuk membedakan mode Detail dan mode Delete (CRUD)
        if (isEditMode) {
            holder.checkDelete.visibility = View.VISIBLE
            holder.imgDetail.visibility = View.GONE // Sembunyikan tombol detail saat mengedit
        } else {
            holder.checkDelete.visibility = View.GONE
            holder.imgDetail.visibility = View.VISIBLE // Tampilkan tombol detail untuk navigasi
            item.isSelected = false
        }

        holder.checkDelete.setOnCheckedChangeListener(null)
        holder.checkDelete.isChecked = item.isSelected

        holder.checkDelete.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
        }

        // Implementasi klik pada item untuk membuka halaman Detail (Syarat Intent)
        holder.itemView.setOnClickListener {
            if (isEditMode) {
                holder.checkDelete.isChecked = !holder.checkDelete.isChecked
            } else {
                // Menjalankan Intent dan mengirim data formula & hasil (CPL 3 - CPMK 3)
                val context = holder.itemView.context
                val intent = Intent(context, DetailActivity::class.java).apply {
                    putExtra("EXTRA_FORMULA", item.formula)
                    putExtra("EXTRA_RESULT", item.result)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = historyList.size

    // Mengubah mode dari HistoryFragment untuk memunculkan checkbox hapus
    fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        notifyDataSetChanged()
    }

    // Mendapatkan item terpilih untuk fungsi Delete di database
    fun getSelectedItems(): List<HistoryModel> = historyList.filter { it.isSelected }

    fun clearSelections() {
        historyList.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }
}