package com.example.boardgame.socket.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerSnapshot {
    private final String id;
    private final String nickname;
    private final int score;
    private final int position;
    private final boolean ready;
    private final boolean host;

    // ⚠️ [추가 포인트 1] 클라이언트 UI 렌더링을 위한 상태 필드 추가
    private final boolean inMicroGame; // 현재 광고 시청 등 마이크로 게임 중인지 여부
    private final List<String> itemCards; // 보유 중인 아이템 카드 목록 (방어권 등)

    public PlayerSnapshot(
            String id,
            String nickname,
            int score,
            int position,
            boolean ready,
            boolean host,
            boolean inMicroGame,
            List<String> itemCards
    ) {
        this.id = id == null ? "" : id;
        this.nickname = nickname == null ? "" : nickname;
        this.score = score;
        this.position = position;
        this.ready = ready;
        this.host = host;
        this.inMicroGame = inMicroGame;
        // null 방지 및 읽기 전용 복사본 생성
        this.itemCards = itemCards == null ? new ArrayList<>() : new ArrayList<>(itemCards);
    }

    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public int getScore() { return score; }
    public int getPosition() { return position; }
    public boolean isReady() { return ready; }
    public boolean isHost() { return host; }

    public boolean isInMicroGame() { return inMicroGame; }
    public List<String> getItemCards() {
        return Collections.unmodifiableList(itemCards);
    }
}