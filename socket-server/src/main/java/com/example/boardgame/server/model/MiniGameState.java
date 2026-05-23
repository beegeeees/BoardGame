package com.example.boardgame.server.model;

import com.example.boardgame.socket.protocol.MiniGameSnapshot;

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
        // 💡 실시간으로 점수(진행도)를 계속 덮어쓸 수 있도록 중복 검사 로직(Exception)을 제거했습니다.
        scoresByPlayerId.put(playerId, score);
    }

    // 💡 서버가 10초(또는 제한시간)가 지났는지 확인할 수 있도록 추가된 유틸리티
    public boolean isTimeUp() {
        return System.currentTimeMillis() - startedAtMillis >= durationMillis;
    }

    // 💡 클라이언트(안드로이드) 화면에 데이터를 쏴주기 위한 스냅샷 생성
    public MiniGameSnapshot toSnapshot() {
        int remainingTime = durationMillis - (int)(System.currentTimeMillis() - startedAtMillis);
        return new MiniGameSnapshot(
                id,
                type,
                status,
                Math.max(0, remainingTime), // 남은 시간이 마이너스가 되지 않도록 방어
                new LinkedHashMap<>(scoresByPlayerId)
        );
    }

    // --- Getters & Setters ---

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