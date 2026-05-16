package com.example.boardgame.socket.protocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public final class SnapshotCodec {
    private static final String PLAYER_SEPARATOR = ";";
    private static final String VALUE_SEPARATOR = ",";

    private SnapshotCodec() {
    }

    public static String encodePlayers(List<PlayerSnapshot> players) {
        if (players == null || players.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (PlayerSnapshot player : players) {
            if (builder.length() > 0) {
                builder.append(PLAYER_SEPARATOR);
            }
            builder.append(encode(player.getId())).append(VALUE_SEPARATOR)
                    .append(encode(player.getNickname())).append(VALUE_SEPARATOR)
                    .append(player.getScore()).append(VALUE_SEPARATOR)
                    .append(player.getPosition()).append(VALUE_SEPARATOR)
                    .append(player.isReady()).append(VALUE_SEPARATOR)
                    .append(player.isHost());
        }
        return builder.toString();
    }

    public static List<PlayerSnapshot> decodePlayers(String encodedPlayers) {
        if (encodedPlayers == null || encodedPlayers.isEmpty()) {
            return Collections.emptyList();
        }

        List<PlayerSnapshot> players = new ArrayList<>();
        String[] playerParts = encodedPlayers.split(PLAYER_SEPARATOR);
        for (String playerPart : playerParts) {
            String[] values = playerPart.split(VALUE_SEPARATOR, -1);
            if (values.length != 6) {
                continue;
            }
            players.add(new PlayerSnapshot(
                    decode(values[0]),
                    decode(values[1]),
                    parseInt(values[2]),
                    parseInt(values[3]),
                    Boolean.parseBoolean(values[4]),
                    Boolean.parseBoolean(values[5])
            ));
        }
        return players;
    }

    public static String encodeIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String id : ids) {
            if (builder.length() > 0) {
                builder.append(VALUE_SEPARATOR);
            }
            builder.append(encode(id));
        }
        return builder.toString();
    }

    public static List<String> decodeIds(String encodedIds) {
        if (encodedIds == null || encodedIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>();
        String[] parts = encodedIds.split(VALUE_SEPARATOR);
        for (String part : parts) {
            ids.add(decode(part));
        }
        return ids;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static int parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(value);
    }
}
