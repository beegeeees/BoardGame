package com.example.boardgame.server;

class DevAuthVerifier implements AuthVerifier {
    private static final String DEV_UID_PREFIX = "dev-";

    @Override
    public String verify(String firebaseIdToken, String connectionId) {
        String token = firebaseIdToken == null ? "" : firebaseIdToken.trim();
        if (!token.isEmpty()) {
            return token;
        }
        return DEV_UID_PREFIX + connectionId;
    }
}
