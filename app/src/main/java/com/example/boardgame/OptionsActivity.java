package com.example.boardgame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OptionsActivity extends AppCompatActivity {
    private TextView nicknameValueText;
    private EditText serverUrlInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        nicknameValueText = findViewById(R.id.nicknameValueText);
        serverUrlInput = findViewById(R.id.serverUrlInput);

        Button changeNicknameButton = findViewById(R.id.changeNicknameButton);
        Button saveServerUrlButton = findViewById(R.id.saveServerUrlButton);
        Button useLanServerButton = findViewById(R.id.useLanServerButton);
        Button useWanServerButton = findViewById(R.id.useWanServerButton);
        Button closeButton = findViewById(R.id.closeOptionsButton);

        changeNicknameButton.setOnClickListener(
                view -> startActivity(new Intent(this, NicknameActivity.class))
        );
        closeButton.setOnClickListener(view -> finish());

        saveServerUrlButton.setOnClickListener(view -> saveServerUrlFromInput());
        useLanServerButton.setOnClickListener(view -> {
            ServerSession.useDefaultLan(this);
            renderServerUrl();
            Toast.makeText(this, R.string.options_server_url_saved, Toast.LENGTH_SHORT).show();
        });
        useWanServerButton.setOnClickListener(view -> {
            ServerSession.useDefaultWan(this);
            renderServerUrl();
            Toast.makeText(this, R.string.options_server_url_saved, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderNickname();
        renderServerUrl();
    }

    private void renderNickname() {
        String nickname = SessionPrefs.getNickname(this);
        nicknameValueText.setText(nickname == null || nickname.trim().isEmpty()
                ? getString(R.string.options_nickname_not_set)
                : nickname);
    }

    private void renderServerUrl() {
        serverUrlInput.setText(ServerSession.getServerUrl(this));
    }

    private void saveServerUrlFromInput() {
        String wsUrl = serverUrlInput.getText().toString().trim();
        if (!(wsUrl.startsWith("ws://") || wsUrl.startsWith("wss://"))) {
            serverUrlInput.setError(getString(R.string.options_server_url_invalid));
            return;
        }
        String normalizedUrl = ServerSession.normalizeServerUrl(wsUrl);
        ServerSession.setServerUrl(this, normalizedUrl);
        serverUrlInput.setText(normalizedUrl);
        Toast.makeText(this, R.string.options_server_url_saved, Toast.LENGTH_SHORT).show();
    }
}
