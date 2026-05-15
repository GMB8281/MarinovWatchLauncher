package com.marinov.watchlauncher

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Botão de voltar simples para sair das configurações
        val btnBack = findViewById<ImageView>(R.id.btnSettingsBack)
        btnBack.setOnClickListener {
            finish()
        }
    }
}