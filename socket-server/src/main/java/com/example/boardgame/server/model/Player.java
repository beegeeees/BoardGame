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

    // ⚠️ [추가 포인트 1] 광고 칸(마이크로 게임) 등에서 턴을 멈추고 대기 중인지 확인하는 플래그
    private boolean inMicroGame = false;

    // 카드는 최대 1장만 가질 수 있도록 제한 로직을 추가할 예정입니다.
    private final List<String> itemCards = new ArrayList<>();

    public Player(String id, String firebaseUid, String nickname) {
        this.id = id;
        this.firebaseUid = firebaseUid == null ? "" : firebaseUid;
        this.nickname = nickname == null || nickname.trim().isEmpty() ? "Player" : nickname.trim();
    }

    public void moveBy(int steps, int boardSize) {
        // Math.floorMod를 사용해 뒤로 가는 효과(음수 steps)가 발생해도 안전하게 순환합니다.
        position = Math.floorMod(position + steps, boardSize);
    }

    public void addScore(int points) {
        score += points; // 점수는 기획상 마이너스가 될 수 있으므로 그대로 더합니다.
    }

    // ⚠️ [수정 포인트 1] 카드 획득 로직 강화 (최대 1장 제한)
    public boolean addItemCard(String itemCard) {
        if (itemCards.size() >= 1) {
            return false; // 이미 카드가 있으면 획득 실패
        }
        itemCards.add(itemCard);
        return true; // 획득 성공
    }

    // ⚠️ [추가 포인트 2] 특정 카드를 가지고 있는지 확인 (예: 방어권)
    public boolean hasItemCard(String itemCard) {
        return itemCards.contains(itemCard);
    }

    // ⚠️ [추가 포인트 3] 카드 사용(소모) 로직
    public boolean useItemCard(String itemCard) {
        return itemCards.remove(itemCard);
    }

    public PlayerSnapshot toSnapshot() {
        // ⚠️ [수정 완료] PlayerSnapshot 생성자 인자 개수(8개)에 맞게 데이터 추가
        return new PlayerSnapshot(
                id,
                nickname,
                score,
                position,
                ready,
                host,
                inMicroGame,
                new ArrayList<>(itemCards)
        );
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public String getFirebaseUid() { return firebaseUid; }
    public String getNickname() { return nickname; }
    public int getScore() { return score; }

    // 점수 강제 세팅이 필요한 경우를 대비 (예: 미니게임 후 일괄 정산)
    public void setScore(int score) { this.score = score; }

    public int getPosition() { return position; }

    // 특정 칸으로 강제 이동(워프)하는 경우를 대비
    public void setPosition(int position) { this.position = position; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }

    public boolean isInMicroGame() { return inMicroGame; }
    public void setInMicroGame(boolean inMicroGame) { this.inMicroGame = inMicroGame; }

    public List<String> getItemCards() {
        return Collections.unmodifiableList(itemCards);
    }
}
