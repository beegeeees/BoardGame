package com.example.boardgame.server;

class LanAuthVerifier implements AuthVerifier {
    @Override
    public String verify(String firebaseIdToken, String fallbackConnectionId) {
        // LAN mode intentionally does not trust or parse tokens. Replace this
        // with Firebase Admin SDK verification before using the server on WAN.
        return "lan-" + fallbackConnectionId;
    }
}
