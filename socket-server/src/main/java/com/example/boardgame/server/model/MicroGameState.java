package com.example.boardgame.server.model;

// 멤버 1, 2가 정의한 프로토콜(shared 폴더)에 맞춰 스냅샷을 만들기 위해 import 필요
import com.example.boardgame.socket.protocol.MicroGameSnapshot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MicroGameState {
    public static final String RUNNING = "RUNNING";
    public static final String FINISHED = "FINISHED";

    // ⚠️ [추가 포인트] 기획서 기준 마이크로게임(광고) 타입 상수
    public static final String TYPE_AD_WATCH = "AD_WATCH";

    private final String id;
    private final String type;
    private final String triggerPlayerId; // 광고 칸을 밟은 장본인 ID
    private final long startedAtMillis;
    private final int durationMillis; // 기획서 기준 10초 (10_000)
    private String status = RUNNING;

    // 광고의 경우 점수가 아니라 성공(1) / 실패(0) 여부를 담는 용도로 사용될 수 있습니다.
    private final Map<String, Integer> scoresByPlayerId = new LinkedHashMap<>();

    public MicroGameState(
            String id,
            String type,
            String triggerPlayerId,
            long startedAtMillis,
            int durationMillis
    ) {
        this.id = id;
        this.type = type;
        this.triggerPlayerId = triggerPlayerId;
        this.startedAtMillis = startedAtMillis;
        this.durationMillis = durationMillis;
    }

    public void submitScore(String playerId, int score) {
        if (!RUNNING.equals(status)) {
            throw new IllegalStateException("Micro game is not running");
        }

        // ⚠️ [수정 포인트 1] 중복 제출 예외 제거
        // 기존 코드: if (scoresByPlayerId.containsKey(playerId)) throw new Exception(...);
        // 클라이언트가 광고 시청 중간중간 "진행 중"이라는 핑(Ping)을 보내거나,
        // 성공/실패 여부를 재전송할 수 있으므로 값을 덮어쓰도록 허용합니다.
        scoresByPlayerId.put(playerId, score);
    }

    // ⚠️ [추가 포인트 1] 기획서 룰에 맞춘 제한 시간 10초 경과 확인
    public boolean isTimeUp() {
        return System.currentTimeMillis() - startedAtMillis >= durationMillis;
    }

    // ⚠️ [추가 포인트 2] 클라이언트 전송용 스냅샷 생성
    public MicroGameSnapshot toSnapshot() {
        // 남은 시간(밀리초) 계산 (0보다 밑으로 내려가지 않게 보정)
        int remainingTime = durationMillis - (int)(System.currentTimeMillis() - startedAtMillis);

        return new MicroGameSnapshot(
                id,
                type,
                triggerPlayerId,
                status,
                Math.max(0, remainingTime),
                new LinkedHashMap<>(scoresByPlayerId)
        );
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public String getType() { return type; }
    public String getTriggerPlayerId() { return triggerPlayerId; }
    public long getStartedAtMillis() { return startedAtMillis; }
    public int getDurationMillis() { return durationMillis; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Integer> getScoresByPlayerId() {
        return Collections.unmodifiableMap(scoresByPlayerId);
    }
}
