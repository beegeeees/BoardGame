package com.example.boardgame.server;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DevAuthVerifierTest {
    @Test
    public void verifyUsesTokenAsUidWhenProvided() {
        DevAuthVerifier verifier = new DevAuthVerifier();

        assertEquals("dev-user-1", verifier.verify(" dev-user-1 ", "connection-1"));
    }

    @Test
    public void verifyFallsBackToConnectionUidWhenTokenIsMissing() {
        DevAuthVerifier verifier = new DevAuthVerifier();

        assertEquals("dev-connection-1", verifier.verify("", "connection-1"));
    }
}
