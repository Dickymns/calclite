package com.nimsaya.calclite.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import com.nimsaya.calclite.R
import com.nimsaya.calclite.databinding.ActivityMainBinding
import com.nimsaya.calclite.fragments.HistoryFragment
import com.nimsaya.calclite.models.HistoryModel
import net.objecthunter.exp4j.ExpressionBuilder
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var lastNumeric = false
    private var stateError = false
    private var lastDot = false
    private var isUpdating = false

    private val dbHistory = FirebaseDatabase.getInstance().getReference("history")
    private val CHANNEL_ID = "calc_history_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtDisplay.setHorizontallyScrolling(true)
        binding.txtDisplay.movementMethod = ScrollingMovementMethod()
        binding.txtDisplay.isHorizontalScrollBarEnabled = false

        setupTextFormatter()
        createNotificationChannel()
        checkNotificationPermission()
        resetToAC()
        setupButtons()

        binding.btnToggleMenu.setOnClickListener {
            val fragment = HistoryFragment()
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupTextFormatter() {
        binding.txtDisplay.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s.isNullOrEmpty() || stateError) return

                isUpdating = true
                val originalText = s.toString()

                // ✅ PERBAIKAN REGEX: Tambahkan \\- agar operator minus dikenali sebagai pemisah
                val operators = Regex("[+×÷\\-]")
                val parts = originalText.split(operators)
                val operatorSymbols = operators.findAll(originalText).map { it.value }.toList()

                val formattedResult = StringBuilder()
                for (i in parts.indices) {
                    val part = parts[i]
                    if (part.isEmpty()) {
                        if (i < operatorSymbols.size) formattedResult.append(operatorSymbols[i])
                        continue
                    }

                    val hasPercent = part.endsWith("%")
                    // Cek angka negatif dalam kurung
                    val isBracketNegative = part.startsWith("(-") && (part.endsWith(")") || part.endsWith("%)"))

                    // Bersihkan untuk format ribuan
                    val cleanPart = part.replace(".", "").replace("(", "").replace("-", "").replace(")", "").replace("%", "")

                    if (cleanPart.isNotEmpty() && cleanPart.all { it.isDigit() }) {
                        val formatted = formatNumber(cleanPart.toDouble())
                        val resultPart = if (isBracketNegative) "(-$formatted)" else formatted
                        formattedResult.append(if (hasPercent) "$resultPart%" else resultPart)
                    } else {
                        formattedResult.append(part)
                    }

                    if (i < operatorSymbols.size) formattedResult.append(operatorSymbols[i])
                }

                if (originalText != formattedResult.toString()) {
                    binding.txtDisplay.setText(formattedResult.toString())
                }

                isUpdating = false
                scrollDisplayToEnd()
            }
        })
    }

    private fun setupButtons() {
        val numericButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )

        numericButtons.forEach { button ->
            button.setOnClickListener {
                if (stateError) {
                    binding.txtDisplay.text = button.text
                    stateError = false
                } else {
                    val current = binding.txtDisplay.text.toString()
                    if (current == "0") binding.txtDisplay.text = button.text
                    else binding.txtDisplay.append(button.text)
                }
                lastNumeric = true
                switchToBackspaceIcon()
            }
        }

        binding.btnPlus.setOnClickListener { appendOperator("+") }
        binding.btnMinus.setOnClickListener { appendOperator("-") } // ✅ Tombol Minus
        binding.btnMultiply.setOnClickListener { appendOperator("×") }
        binding.btnDivide.setOnClickListener { appendOperator("÷") }

        binding.btnPercent.setOnClickListener {
            if (lastNumeric && !stateError) {
                binding.txtDisplay.append("%")
                lastNumeric = false
                switchToBackspaceIcon()
            }
        }

        binding.btnPlusMinus.setOnClickListener {
            if (stateError) return@setOnClickListener
            val currentText = binding.txtDisplay.text.toString()
            if (currentText == "0" || currentText.isEmpty()) return@setOnClickListener

            // ✅ Gunakan regex yang sama agar konsisten
            val operators = Regex("[+×÷\\-]")
            val parts = currentText.split(operators).toMutableList()
            val lastPart = parts.last()

            if (lastPart.isNotEmpty()) {
                val toggledPart = if (lastPart.startsWith("(-") && lastPart.endsWith(")")) {
                    lastPart.replace("(", "").replace("-", "").replace(")", "")
                } else {
                    "(-$lastPart)"
                }
                parts[parts.size - 1] = toggledPart

                val operatorMatches = operators.findAll(currentText).map { it.value }.toList()
                val newText = StringBuilder()
                for (i in parts.indices) {
                    newText.append(parts[i])
                    if (i < operatorMatches.size) newText.append(operatorMatches[i])
                }
                binding.txtDisplay.text = newText.toString()
            }
        }

        binding.btnClear.setOnClickListener {
            val text = binding.txtDisplay.text.toString()
            if (text.length > 1 && text != "Error") {
                binding.txtDisplay.text = text.dropLast(1)
            } else {
                resetToAC()
            }
        }

        binding.btnEqual.setOnClickListener { onEqual() }
    }

    private fun appendOperator(op: String) {
        if (lastNumeric && !stateError) {
            binding.txtDisplay.append(op)
            lastNumeric = false
            lastDot = false
        }
    }

    private fun onEqual() {
        if (!lastNumeric || stateError) return
        val displayFormula = binding.txtDisplay.text.toString()

        // ✅ PERBAIKAN EVALUASI: Kembalikan format kurung ke format matematika standar
        // EkspresiBuilder butuh (-88) tetap ada atau diubah menjadi -88
        val mathFormula = displayFormula.replace(".", "")
            .replace("(", "").replace(")", "") // Sederhanakan kurung untuk hitungan
            .replace(",", ".")
            .replace("×", "*")
            .replace("÷", "/")

        try {
            val result = ExpressionBuilder(mathFormula.replace("%", "/100")).build().evaluate()
            val formatted = formatNumber(result)

            if (result < 0) binding.txtDisplay.text = "(-${formatted.replace("-", "")})"
            else binding.txtDisplay.text = formatted

            lastNumeric = true
            saveCalculationToFirebase(displayFormula, formatted)
        } catch (e: Exception) {
            binding.txtDisplay.text = "Error"
            stateError = true
        }
    }

    private fun scrollDisplayToEnd() {
        binding.txtDisplay.post {
            val layout = binding.txtDisplay.layout ?: return@post
            val textWidth = layout.getLineRight(0).toInt()
            val viewWidth = binding.txtDisplay.width - binding.txtDisplay.paddingLeft - binding.txtDisplay.paddingRight

            if (textWidth > viewWidth) binding.txtDisplay.scrollTo(textWidth - viewWidth, 0)
            else binding.txtDisplay.scrollTo(0, 0)
        }
    }

    private fun formatNumber(number: Double): String {
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        return DecimalFormat("#,###.########", symbols).format(number)
    }

    private fun resetToAC() {
        binding.txtDisplay.text = "0"
        lastNumeric = false
        stateError = false
        lastDot = false
        binding.btnClear.text = "AC"
        binding.btnClear.setPadding(0, 0, 0, 0)
        binding.btnClear.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        binding.btnClear.gravity = Gravity.CENTER
        binding.txtDisplay.scrollTo(0, 0)
    }

    private fun switchToBackspaceIcon() {
        binding.btnClear.text = ""
        binding.btnClear.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_backspace, 0, 0, 0)
        val density = resources.displayMetrics.density
        val paddingLeft = (26 * density).toInt()
        binding.btnClear.setPadding(paddingLeft, 0, 0, 0)
        binding.btnClear.gravity = Gravity.CENTER_VERTICAL
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "History Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun saveCalculationToFirebase(formula: String, result: String) {
        val id = dbHistory.push().key ?: return
        dbHistory.child(id).setValue(HistoryModel(id, formula, result))
            .addOnSuccessListener {
                showNotification(formula, result)
            }
    }

    private fun showNotification(formula: String, result: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu)
            .setContentTitle("Riwayat Tersimpan")
            .setContentText("$formula = $result")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}