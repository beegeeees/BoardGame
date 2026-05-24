package com.example.adchaosdemo

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MiniGame3IntroActivity : AppCompatActivity() {

    private var introCountDownTimer: CountDownTimer? = null
    private var lastShownCount = Int.MIN_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_minigame3_intro)

        val countdownText = findViewById<TextView>(R.id.introCountdownText)
        lastShownCount = 3
        countdownText.text = lastShownCount.toString()

        introCountDownTimer = object : CountDownTimer(3000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = ((millisUntilFinished + 999L) / 1000L).toInt()
                if (remaining in 1..2 && remaining != lastShownCount) {
                    lastShownCount = remaining
                    countdownText.text = remaining.toString()
                }
            }

            override fun onFinish() {
                startActivity(Intent(this@MiniGame3IntroActivity, MiniGame3Activity::class.java))
                finish()
            }
        }.start()
    }

    override fun onDestroy() {
        introCountDownTimer?.cancel()
        introCountDownTimer = null
        lastShownCount = Int.MIN_VALUE
        super.onDestroy()
    }
}
