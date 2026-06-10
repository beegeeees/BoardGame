package com.example.boardgame.server.service;

import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScoreService {

    /**
     * 미니게임/마이크로게임에서 제출된 원시 점수(rawScores)를 바탕으로
     * 등수를 매기고, 준비된 등수별 보상(rewardsByRank)을 매핑합니다.
     */
    public Map<String, Integer> rankScores(Map<String, Integer> rawScoresByPlayerId, int[] rewardsByRank) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(rawScoresByPlayerId.entrySet());

        // 점수가 높은 순으로 정렬하되, 같은 점수는 같은 순위 보상을 받습니다.
        entries.sort((first, second) -> Integer.compare(second.getValue(), first.getValue()));

        Map<String, Integer> rewards = new LinkedHashMap<>();
        int rankIndex = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0 && !entries.get(i).getValue().equals(entries.get(i - 1).getValue())) {
                rankIndex = i;
            }
            int reward = rankIndex < rewardsByRank.length ? rewardsByRank[rankIndex] : 0;
            rewards.put(entries.get(i).getKey(), reward);
        }
        return rewards;
    }

    /**
     * 계산된 보상 점수를 실제 방에 있는 플레이어들의 총점에 반영합니다.
     */
    public void applyRewards(Room room, Map<String, Integer> rewardsByPlayerId) {
        for (Map.Entry<String, Integer> entry : rewardsByPlayerId.entrySet()) {
            Player player = room.getPlayers().get(entry.getKey());
            if (player != null) {
                // 플레이어의 기존 총점에 보상 점수를 누적
                player.addScore(entry.getValue());
            }
        }
        room.touch(); // 방 상태 변경 알림
    }

    //  게임 3라운드 최종 종료 시 최종 순위 계산
    /**
     * 보드게임이 모두 끝났을 때(FINISHED 상태),
     * 현재 플레이어들의 최종 총점을 비교하여 1등부터 꼴등까지의 순위를 반환합니다.
     */
    public List<Player> calculateFinalRankings(Room room) {
        List<Player> players = new ArrayList<>(room.getPlayerList());

        // 플레이어의 현재 총점(Score)을 기준으로 내림차순 정렬
        players.sort((first, second) -> Integer.compare(second.getScore(), first.getScore()));

        return players; // 0번 인덱스가 최종 우승자
    }
}
