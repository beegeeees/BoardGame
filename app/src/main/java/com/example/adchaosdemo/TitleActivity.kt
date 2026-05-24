package com.example.adchaosdemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class TitleActivity : AppCompatActivity() {

    private lateinit var nicknameBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_title)

        nicknameBadge = findViewById(R.id.nicknameBadge)
        val joinGameButton = findViewById<Button>(R.id.joinGameButton)
        val optionsButton = findViewById<Button>(R.id.optionsButton)
        val rulesButton = findViewById<Button>(R.id.rulesButton)

        joinGameButton.setOnClickListener { onJoinGameClicked() }
        optionsButton.setOnClickListener { startActivity(Intent(this, OptionsActivity::class.java)) }
        rulesButton.setOnClickListener { showRulesPagerDialog() }
        nicknameBadge.setOnClickListener {
            startActivity(Intent(this, NicknameActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        renderNickname()
    }

    private fun onJoinGameClicked() {
        val nickname = SessionPrefs.getNickname(this)
        val destination = if (nickname.isBlank()) SplashActivity::class.java else LobbyListActivity::class.java
        startActivity(Intent(this, destination))
    }

    private fun renderNickname() {
        val nickname = SessionPrefs.getNickname(this)
        if (nickname.isBlank()) {
            nicknameBadge.visibility = View.GONE
            return
        }

        nicknameBadge.visibility = View.VISIBLE
        nicknameBadge.text = getString(R.string.title_nickname_format, nickname)
    }

    private fun showRulesPagerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rules_pager, null)
        val pageText = dialogView.findViewById<TextView>(R.id.rulesPageText)
        val pageIndexText = dialogView.findViewById<TextView>(R.id.rulesPageIndexText)
        val prevButton = dialogView.findViewById<ImageButton>(R.id.rulesPrevButton)
        val nextButton = dialogView.findViewById<ImageButton>(R.id.rulesNextButton)
        val pages = resources.getStringArray(R.array.title_rules_pages_v2)
        var index = 0

        fun renderPage() {
            pageText.text = pages[index]
            pageIndexText.text = "${index + 1} / ${pages.size}"
            prevButton.isEnabled = index > 0
            nextButton.isEnabled = index < pages.lastIndex
            prevButton.alpha = if (prevButton.isEnabled) 1.0f else 0.35f
            nextButton.alpha = if (nextButton.isEnabled) 1.0f else 0.35f
        }

        prevButton.setOnClickListener {
            if (index > 0) {
                index -= 1
                renderPage()
            }
        }
        nextButton.setOnClickListener {
            if (index < pages.lastIndex) {
                index += 1
                renderPage()
            }
        }

        renderPage()
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .show()
    }
}
