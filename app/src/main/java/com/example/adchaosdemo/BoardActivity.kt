package com.example.adchaosdemo

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BoardActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYERS = "extra_players"
    }

    private var startCountDownTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var moveToMiniGameRunnable: Runnable? = null
    private var lastShownCount = Int.MIN_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board)

        val boardCountdownText = findViewById<TextView>(R.id.boardCountdownText)
        val boardPlayersText = findViewById<TextView>(R.id.boardPlayersText)
        val players = intent.getStringArrayListExtra(EXTRA_PLAYERS).orEmpty()

        val description = buildString {
            append(getString(R.string.board_demo_title))
            append("\n\n")
            append(getString(R.string.board_participants_title))
            append("\n")
            players.forEachIndexed { index, name ->
                append("${index + 1}P  $name\n")
            }
        }
        boardPlayersText.text = description

        lastShownCount = 5
        boardCountdownText.text = getString(R.string.board_start_countdown, lastShownCount)
        startCountDownTimer = object : CountDownTimer(5000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = ((millisUntilFinished + 999L) / 1000L).toInt()
                if (remainingSeconds in 1..4 && remainingSeconds != lastShownCount) {
                    lastShownCount = remainingSeconds
                    boardCountdownText.text = getString(R.string.board_start_countdown, remainingSeconds)
                }
            }

            override fun onFinish() {
                boardCountdownText.text = getString(R.string.board_start_now)
                moveToMiniGameRunnable = Runnable {
                    startActivity(Intent(this@BoardActivity, MiniGame3IntroActivity::class.java))
                    finish()
                }.also { handler.postDelayed(it, 1500L) }
            }
        }.start()
    }

    override fun onDestroy() {
        startCountDownTimer?.cancel()
        startCountDownTimer = null
        lastShownCount = Int.MIN_VALUE
        moveToMiniGameRunnable?.let { handler.removeCallbacks(it) }
        moveToMiniGameRunnable = null
        super.onDestroy()
    }
}
