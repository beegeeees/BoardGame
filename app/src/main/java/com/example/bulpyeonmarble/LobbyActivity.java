package com.example.bulpyeonmarble;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class LobbyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(LobbyActivity.this, BoardActivity.class);
            startActivity(intent);
        });
    }
}