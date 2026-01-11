package com.nimsaya.calclite.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nimsaya.calclite.adapters.HistoryAdapter
import com.nimsaya.calclite.databinding.FragmentHistorySheetBinding
import com.nimsaya.calclite.models.HistoryModel

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistorySheetBinding? = null
    private val binding get() = _binding!!
    private val dbHistory = FirebaseDatabase.getInstance().getReference("history")
    private var isEditMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistorySheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView (Syarat List/RecyclerView)
        binding.rvHistory.layoutManager = LinearLayoutManager(context)

        // 2. Tombol Selesai (Navigasi Fragment)
        binding.btnSelesai.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 3. Tombol Edit (Syarat CRUD: Update Mode)
        binding.btnEdit.setOnClickListener {
            isEditMode = !isEditMode
            binding.btnEdit.text = if (isEditMode) "Batal" else "Edit Item"
            binding.btnBersihkan.text = if (isEditMode) "Hapus" else "Hapus Semua"

            // Ubah warna teks bersihkan saat mode hapus aktif
            binding.btnBersihkan.setTextColor(
                Color.parseColor(if (isEditMode) "#FF3B30" else "#FF9F0A")
            )

            // Mengirim status edit ke adapter agar checkbox muncul
            (binding.rvHistory.adapter as? HistoryAdapter)?.setEditMode(isEditMode)
        }

        // 4. Tombol Bersihkan/Hapus (Syarat CRUD: Delete)
        binding.btnBersihkan.setOnClickListener {
            if (isEditMode) {
                // Hapus item yang dipilih saja
                val adapter = binding.rvHistory.adapter as? HistoryAdapter
                adapter?.getSelectedItems()?.forEach {
                    dbHistory.child(it.id).removeValue()
                }
            } else {
                // Konfirmasi Hapus Semua (Opsional: agar terlihat lebih profesional)
                AlertDialog.Builder(requireContext())
                    .setTitle("Hapus Riwayat")
                    .setMessage("Apakah Anda yakin ingin menghapus semua riwayat?")
                    .setPositiveButton("Ya") { _, _ -> dbHistory.removeValue() }
                    .setNegativeButton("Tidak", null)
                    .show()
            }
        }

        // 5. READ Data dari Firebase Realtime Database (Syarat CRUD: Read)
        dbHistory.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull {
                    it.getValue(HistoryModel::class.java)
                }
                // Urutkan dari yang terbaru ke terlama
                binding.rvHistory.adapter = HistoryAdapter(list.reversed())
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}