package com.example.bulpyeonmarble;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView finalRankText = findViewById(R.id.finalRankText);
        TextView finalScoreText = findViewById(R.id.finalScoreText);

        int totalScore = getIntent().getIntExtra("totalScore", 0);

        int finalRank = 4;
        if (totalScore == 300) {
            finalRank = 1;
        } else if (totalScore == 200) {
            finalRank = 2;
        } else if (totalScore == 100) {
            finalRank = 3;
        }

        finalRankText.setText("최종 순위: " + finalRank + "등");
        finalScoreText.setText("최종 획득 점수: " + totalScore + "점");

        new Handler().postDelayed(() -> {
            if (!isFinishing()) {
                Intent intent = new Intent(ResultActivity.this, BoardActivity.class);
                startActivity(intent);
                finish();
            }
        }, 10000);
    }
}