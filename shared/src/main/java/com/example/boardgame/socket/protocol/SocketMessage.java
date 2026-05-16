package com.example.boardgame.socket.protocol;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SocketMessage {
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_REQUEST_ID = "requestId";

    private final String type;
    private final String requestId;
    private final Map<String, String> fields;

    private SocketMessage(String type, String requestId, Map<String, String> fields) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Message type is required");
        }
        this.type = type;
        this.requestId = requestId == null ? "" : requestId;
        this.fields = new LinkedHashMap<>(fields);
    }

    public static SocketMessage command(String type) {
        return builder(type)
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    public static Builder builder(String type) {
        return new Builder(type);
    }

    public static SocketMessage parse(String wireText) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (wireText != null && !wireText.isEmpty()) {
            String[] pairs = wireText.split("&");
            for (String pair : pairs) {
                int separator = pair.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = decode(pair.substring(0, separator));
                String value = decode(pair.substring(separator + 1));
                fields.put(key, value);
            }
        }

        String type = fields.remove(FIELD_TYPE);
        String requestId = fields.remove(FIELD_REQUEST_ID);
        return new SocketMessage(type, requestId, fields);
    }

    public String toWireText() {
        Map<String, String> orderedFields = new LinkedHashMap<>();
        orderedFields.put(FIELD_TYPE, type);
        orderedFields.put(FIELD_REQUEST_ID, requestId);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!FIELD_TYPE.equals(entry.getKey()) && !FIELD_REQUEST_ID.equals(entry.getKey())) {
                orderedFields.put(entry.getKey(), entry.getValue());
            }
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : orderedFields.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(encode(entry.getKey()));
            builder.append('=');
            builder.append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    public String getType() {
        return type;
    }

    public String getRequestId() {
        return requestId;
    }

    public String get(String key) {
        return fields.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        String value = fields.get(key);
        return value == null ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        String value = fields.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = fields.get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public Map<String, String> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class Builder {
        private final String type;
        private String requestId = "";
        private final Map<String, String> fields = new LinkedHashMap<>();

        private Builder(String type) {
            this.type = type;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder put(String key, String value) {
            if (key != null && !key.trim().isEmpty()) {
                fields.put(key, value == null ? "" : value);
            }
            return this;
        }

        public Builder put(String key, int value) {
            return put(key, String.valueOf(value));
        }

        public Builder put(String key, boolean value) {
            return put(key, String.valueOf(value));
        }

        public SocketMessage build() {
            return new SocketMessage(type, requestId, fields);
        }
    }
}
