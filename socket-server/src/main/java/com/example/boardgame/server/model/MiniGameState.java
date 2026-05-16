package com.example.boardgame.server.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MiniGameState {
    public static final String RUNNING = "RUNNING";
    public static final String FINISHED = "FINISHED";

    private final String id;
    private final String type;
    private final long startedAtMillis;
    private final int durationMillis;
    private String status = RUNNING;
    private final Map<String, Integer> scoresByPlayerId = new LinkedHashMap<>();

    public MiniGameState(String id, String type, long startedAtMillis, int durationMillis) {
        this.id = id;
        this.type = type;
        this.startedAtMillis = startedAtMillis;
        this.durationMillis = durationMillis;
    }

    public void submitScore(String playerId, int score) {
        if (!RUNNING.equals(status)) {
            throw new IllegalStateException("Mini game is not running");
        }
        if (scoresByPlayerId.containsKey(playerId)) {
            throw new IllegalStateException("Score already submitted");
        }
        scoresByPlayerId.put(playerId, score);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public int getDurationMillis() {
        return durationMillis;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Integer> getScoresByPlayerId() {
        return Collections.unmodifiableMap(scoresByPlayerId);
    }
}
