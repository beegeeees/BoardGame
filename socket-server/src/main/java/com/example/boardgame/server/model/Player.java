package com.example.boardgame.server.model;

import com.example.boardgame.socket.protocol.PlayerSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private final String id;
    private final String firebaseUid;
    private final String nickname;
    private int score;
    private int position;
    private boolean ready;
    private boolean host;
    private final List<String> itemCards = new ArrayList<>();

    public Player(String id, String firebaseUid, String nickname) {
        this.id = id;
        this.firebaseUid = firebaseUid == null ? "" : firebaseUid;
        this.nickname = nickname == null || nickname.trim().isEmpty() ? "Player" : nickname.trim();
    }

    public void moveBy(int steps, int boardSize) {
        position = Math.floorMod(position + steps, boardSize);
    }

    public void addScore(int points) {
        score += points;
    }

    public void addItemCard(String itemCard) {
        itemCards.add(itemCard);
    }

    public PlayerSnapshot toSnapshot() {
        return new PlayerSnapshot(id, nickname, score, position, ready, host);
    }

    public String getId() {
        return id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getNickname() {
        return nickname;
    }

    public int getScore() {
        return score;
    }

    public int getPosition() {
        return position;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public List<String> getItemCards() {
        return Collections.unmodifiableList(itemCards);
    }
}
