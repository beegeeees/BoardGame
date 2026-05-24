package com.example.adchaosdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class NicknameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)

        val nicknameInput = findViewById<EditText>(R.id.nicknameInput)
        val saveNicknameButton = findViewById<Button>(R.id.saveNicknameButton)

        val savedNickname = SessionPrefs.getNickname(this)
        if (savedNickname.isNotBlank()) {
            nicknameInput.setText(savedNickname)
            nicknameInput.setSelection(savedNickname.length)
        }

        saveNicknameButton.setOnClickListener {
            val nickname = nicknameInput.text.toString().trim()
            if (nickname.isEmpty()) {
                nicknameInput.error = getString(R.string.error_nickname_required)
                return@setOnClickListener
            }

            SessionPrefs.setNickname(this, nickname)

            startActivity(Intent(this, TitleActivity::class.java))
            finish()
        }
    }
}
