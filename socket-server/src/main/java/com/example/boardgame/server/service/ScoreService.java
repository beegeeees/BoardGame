package com.example.boardgame.server.service;

import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScoreService {
    public Map<String, Integer> rankScores(Map<String, Integer> rawScoresByPlayerId, int[] rewardsByRank) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(rawScoresByPlayerId.entrySet());
        entries.sort((first, second) -> Integer.compare(second.getValue(), first.getValue()));

        Map<String, Integer> rewards = new LinkedHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            int reward = i < rewardsByRank.length ? rewardsByRank[i] : 0;
            rewards.put(entries.get(i).getKey(), reward);
        }
        return rewards;
    }

    public void applyRewards(Room room, Map<String, Integer> rewardsByPlayerId) {
        for (Map.Entry<String, Integer> entry : rewardsByPlayerId.entrySet()) {
            Player player = room.getPlayers().get(entry.getKey());
            if (player != null) {
                player.addScore(entry.getValue());
            }
        }
        room.touch();
    }
}
