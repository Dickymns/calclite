package com.nimsaya.calclite.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nimsaya.calclite.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menerima data dari Intent (Syarat CPL 3)
        val formula = intent.getStringExtra("EXTRA_FORMULA")
        val result = intent.getStringExtra("EXTRA_RESULT")

        binding.txtDetailFormula.text = formula
        binding.txtDetailResult.text = result

        binding.btnBack.setOnClickListener { finish() }
    }
}