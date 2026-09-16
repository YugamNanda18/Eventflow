package com.eventflow.webhook;

import com.eventflow.common.util.HmacUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookEngineServiceTest {

    @Test
    void testHmacSha256SignatureComputation() {
        String data = "1726484400.{\"orderId\":\"ORD-123\",\"amount\":100}";
        String secret = "whsec_demosecretkey1234567890";

        String signature1 = HmacUtils.computeHmacSha256(data, secret);
        String signature2 = HmacUtils.computeHmacSha256(data, secret);

        assertNotNull(signature1);
        assertEquals(64, signature1.length()); // SHA-256 hex string length
        assertEquals(signature1, signature2); // Deterministic
    }
}
