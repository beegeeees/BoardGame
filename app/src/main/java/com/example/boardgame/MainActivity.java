package com.example.boardgame;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.boardgame.controller.socket.SocketRoomController;
import com.example.boardgame.socket.SocketSnapshotMapper;
import com.example.boardgame.socket.protocol.ConnectionState;
import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.PlayerSnapshot;
import com.example.boardgame.socket.protocol.RoomSnapshot;
import com.example.boardgame.socket.protocol.SocketEventListener;
import com.example.boardgame.socket.protocol.SocketMessage;

public class MainActivity extends AppCompatActivity {
    private final SocketRoomController socketController = new SocketRoomController();

    private EditText serverUrlInput;
    private EditText nicknameInput;
    private EditText roomCodeInput;
    private EditText scoreInput;
    private TextView connectionStateText;
    private TextView myPlayerText;
    private TextView roomStateText;
    private TextView gameStateText;
    private TextView eventLogText;

    private String myPlayerId = "";
    private final StringBuilder eventLog = new StringBuilder("Log:");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        bindSocket();
        bindButtons();
    }

    @Override
    protected void onDestroy() {
        socketController.disconnect();
        super.onDestroy();
    }

    private void bindViews() {
        serverUrlInput = findViewById(R.id.serverUrlInput);
        nicknameInput = findViewById(R.id.nicknameInput);
        roomCodeInput = findViewById(R.id.roomCodeInput);
        scoreInput = findViewById(R.id.scoreInput);
        connectionStateText = findViewById(R.id.connectionStateText);
        myPlayerText = findViewById(R.id.myPlayerText);
        roomStateText = findViewById(R.id.roomStateText);
        gameStateText = findViewById(R.id.gameStateText);
        eventLogText = findViewById(R.id.eventLogText);
    }

    private void bindSocket() {
        socketController.setListener(new SocketEventListener() {
            @Override
            public void onStateChanged(ConnectionState state) {
                runOnUiThread(() -> connectionStateText.setText("Connection: " + state.name()));
            }

            @Override
            public void onMessage(SocketMessage message) {
                runOnUiThread(() -> handleSocketMessage(message));
            }

            @Override
            public void onError(Throwable throwable) {
                runOnUiThread(() -> appendLog("Error: " + throwable.getMessage()));
            }
        });
    }

    private void bindButtons() {
        findViewById(R.id.connectButton).setOnClickListener(v ->
                socketController.connect(textOf(serverUrlInput)));
        findViewById(R.id.disconnectButton).setOnClickListener(v ->
                socketController.disconnect());
        findViewById(R.id.createRoomButton).setOnClickListener(v ->
                socketController.createRoom(nickname(), ""));
        findViewById(R.id.joinRoomButton).setOnClickListener(v ->
                socketController.joinRoom(textOf(roomCodeInput), nickname(), ""));
        findViewById(R.id.matchmakeButton).setOnClickListener(v ->
                socketController.matchmake(nickname(), ""));
        findViewById(R.id.readyButton).setOnClickListener(v ->
                socketController.setReady(true));
        findViewById(R.id.unreadyButton).setOnClickListener(v ->
                socketController.setReady(false));
        findViewById(R.id.startGameButton).setOnClickListener(v ->
                socketController.startGame());
        findViewById(R.id.rollDiceButton).setOnClickListener(v ->
                socketController.rollDice());
        findViewById(R.id.applyTileButton).setOnClickListener(v ->
                socketController.applyTileEffect());
        findViewById(R.id.startMiniGameButton).setOnClickListener(v ->
                socketController.startMiniGame("COLOR_GUESSING"));
        findViewById(R.id.submitMiniScoreButton).setOnClickListener(v ->
                socketController.submitMiniGameScore(score()));
        findViewById(R.id.finishMiniGameButton).setOnClickListener(v ->
                socketController.finishMiniGame());
        findViewById(R.id.submitMicroScoreButton).setOnClickListener(v ->
                socketController.submitMicroGameScore(score()));
        findViewById(R.id.finishMicroGameButton).setOnClickListener(v ->
                socketController.finishMicroGame());
    }

    private void handleSocketMessage(SocketMessage message) {
        if (MessageTypes.REQUEST_OK.equals(message.getType())) {
            myPlayerId = message.getOrDefault("playerId", myPlayerId);
            String roomCode = message.getOrDefault("roomCode", "");
            if (!roomCode.isEmpty()) {
                roomCodeInput.setText(roomCode);
            }
            myPlayerText.setText("Player: " + shortId(myPlayerId));
            appendLog("OK " + message.getOrDefault("status", ""));
        } else if (MessageTypes.REQUEST_ERROR.equals(message.getType())) {
            appendLog("Request error: " + message.getOrDefault("details", ""));
        } else if (MessageTypes.ROOM_UPDATED.equals(message.getType())) {
            renderRoom(SocketSnapshotMapper.toRoomSnapshot(message));
        } else if (MessageTypes.GAME_UPDATED.equals(message.getType())) {
            renderGame(SocketSnapshotMapper.toGameSnapshot(message));
        } else if (!MessageTypes.APP_PONG.equals(message.getType())) {
            appendLog("Message: " + message.getType());
        }
    }

    private void renderRoom(RoomSnapshot room) {
        StringBuilder builder = new StringBuilder();
        builder.append("Room ").append(room.getCode())
                .append(" / ").append(room.getStatus())
                .append("\nHost: ").append(shortId(room.getHostPlayerId()));
        for (PlayerSnapshot player : room.getPlayers()) {
            builder.append("\n")
                    .append(player.isHost() ? "* " : "- ")
                    .append(player.getNickname())
                    .append(" [").append(shortId(player.getId())).append("]")
                    .append(" ready=").append(player.isReady())
                    .append(" pos=").append(player.getPosition())
                    .append(" score=").append(player.getScore());
        }
        roomStateText.setText(builder.toString());
    }

    private void renderGame(GameSnapshot game) {
        gameStateText.setText("Game room=" + game.getRoomCode()
                + "\nround=" + game.getCurrentRound() + "/" + game.getFinalRound()
                + "\nphase=" + game.getTurnPhase()
                + "\ncurrent=" + shortId(game.getCurrentPlayerId())
                + "\ndice=" + game.getLastDiceRoll());
    }

    private void appendLog(String line) {
        eventLog.append("\n").append(line);
        eventLogText.setText(eventLog.toString());
    }

    private String nickname() {
        String nickname = textOf(nicknameInput);
        return nickname.isEmpty() ? "Player" : nickname;
    }

    private int score() {
        String scoreText = textOf(scoreInput);
        if (scoreText.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(scoreText);
        } catch (NumberFormatException e) {
            appendLog("Invalid score: " + scoreText);
            return 0;
        }
    }

    private String textOf(EditText editText) {
        return editText.getText().toString().trim();
    }

    private String shortId(String id) {
        if (id == null || id.isEmpty()) {
            return "-";
        }
        return id.length() <= 6 ? id : id.substring(0, 6);
    }
}
