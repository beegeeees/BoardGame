package com.example.bulpyeonmarble;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BoardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        Button btnStartPasswordGame = findViewById(R.id.btnStartPasswordGame);
        Button btnStartAdGame1 = findViewById(R.id.btnStartAdGame1);
        Button btnStartAdGame2 = findViewById(R.id.btnStartAdGame2);
        Button btnStartAdGame3 = findViewById(R.id.btnStartAdGame3);
        Button btnStartDiceGame = findViewById(R.id.btnStartDiceGame);

        btnStartPasswordGame.setOnClickListener(v -> {
            startActivity(new Intent(BoardActivity.this, PasswordActivity.class));
        });

        btnStartAdGame1.setOnClickListener(v -> {
            startActivity(new Intent(BoardActivity.this, AdGame1Activity.class));
        });

        btnStartAdGame2.setOnClickListener(v -> {
            startActivity(new Intent(BoardActivity.this, AdGame2Activity.class));
        });

        btnStartAdGame3.setOnClickListener(v -> {
            startActivity(new Intent(BoardActivity.this, AdGame3Activity.class));
        });

        btnStartDiceGame.setOnClickListener(v -> {
            startActivity(new Intent(BoardActivity.this, DiceActivity.class));
        });

        int miniGameScore = getIntent().getIntExtra("miniGameScore", -1);
        if (miniGameScore != -1) {
            Toast.makeText(this, "미니게임 점수 반영: " + miniGameScore + "점", Toast.LENGTH_SHORT).show();
        }

        int diceResult = getIntent().getIntExtra("diceResult", -1);
        if (diceResult != -1) {
            Toast.makeText(this, "주사위 결과: " + diceResult + "칸 이동!", Toast.LENGTH_SHORT).show();
        }
    }
}