package com.example.boardgame.server.model;

import com.example.boardgame.socket.protocol.GameSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {

    // --- 턴 페이즈 (Turn Phase) 상수 정의 ---
    public static final String WAITING_FOR_ROLL = "WAITING_FOR_ROLL";           // 주사위 굴리기 대기 중
    public static final String TILE_EFFECT_APPLIED = "TILE_EFFECT_APPLIED";     // 이동 후 타일 효과 적용됨
    public static final String WAITING_FOR_MICRO_GAME = "WAITING_FOR_MICRO_GAME"; // 광고 칸 등 개별 타일 게임 진행 중
    public static final String MINI_GAME_PHASE = "MINI_GAME_PHASE";             // 라운드 종료 후 전체 미니게임 진행 중
    public static final String FINISHED = "FINISHED";                           // 3라운드 종료 및 최종 결과 산출

    private final String roomCode;
    private final int finalRound; // 기획서 기준 3
    private int currentRound = 1;
    private int currentPlayerIndex;
    private int lastDiceRoll;
    private String turnPhase = WAITING_FOR_ROLL;

    // 클라이언트 화면에 띄워줄 시스템 메시지
    private String lastSystemMessage = "";

    // 게임에 참여하는 플레이어들의 ID 순서
    private final List<String> turnOrder = new ArrayList<>();

    public GameState(String roomCode, int finalRound) {
        this.roomCode = roomCode;
        this.finalRound = finalRound > 0 ? finalRound : 3;
    }

    public void setTurnOrder(List<String> playerIds) {
        turnOrder.clear();
        if (playerIds != null) {
            turnOrder.addAll(playerIds);
        }
        currentPlayerIndex = 0;
    }

    public String getCurrentPlayerId() {
        if (turnOrder.isEmpty()) {
            return "";
        }
        return turnOrder.get(currentPlayerIndex);
    }

    public void advanceTurn() {
        if (turnOrder.isEmpty()) {
            return;
        }

        if (currentPlayerIndex == turnOrder.size() - 1) {
            currentPlayerIndex = 0;
            turnPhase = MINI_GAME_PHASE; // 라운드 끝 -> 미니게임
        } else {
            currentPlayerIndex++;
            turnPhase = WAITING_FOR_ROLL; // 다음 사람 턴
        }
    }

    public void advanceRound() {
        currentRound++;
        if (currentRound > finalRound) {
            turnPhase = FINISHED;
        } else {
            currentPlayerIndex = 0;
            turnPhase = WAITING_FOR_ROLL;
        }
    }

    public GameSnapshot toSnapshot() {
        return new GameSnapshot(
                roomCode,
                currentRound,
                finalRound,
                getCurrentPlayerId(),
                lastDiceRoll,
                turnPhase,
                turnOrder,
                lastSystemMessage // 새로 추가된 메시지 전송
        );
    }

    // --- Getters & Setters ---

    public String getRoomCode() { return roomCode; }
    public int getCurrentRound() { return currentRound; }
    public int getFinalRound() { return finalRound; }

    public int getLastDiceRoll() { return lastDiceRoll; }
    public void setLastDiceRoll(int lastDiceRoll) { this.lastDiceRoll = lastDiceRoll; }

    public String getTurnPhase() { return turnPhase; }
    public void setTurnPhase(String turnPhase) { this.turnPhase = turnPhase; }

    public List<String> getTurnOrder() { return Collections.unmodifiableList(turnOrder); }

    public String getLastSystemMessage() { return lastSystemMessage; }
    public void setLastSystemMessage(String lastSystemMessage) { this.lastSystemMessage = lastSystemMessage; }
}