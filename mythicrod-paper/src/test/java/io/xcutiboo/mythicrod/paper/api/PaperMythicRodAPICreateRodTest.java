package io.xcutiboo.mythicrod.paper.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.xcutiboo.mythicrod.api.Result;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;

class PaperMythicRodAPICreateRodTest {

    @Test
    void unknownTierReturnsFailureBeforeTouchingFactory() {
        PaperMythicRodAPI api = new PaperMythicRodAPI(
            "test-version",
            java.util.logging.Logger.getAnonymousLogger(),
            null, null, null, null, null
        );

        Result<PlatformItem> result = api.createRod("god-tier");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("Unknown rod tier"));
        assertTrue(result.getError().contains("basic"));
        assertTrue(result.getError().contains("advanced"));
        assertTrue(result.getError().contains("legendary"));
    }

    @Test
    void unknownTierIsCaseInsensitiveOnTheReportedInput() {
        PaperMythicRodAPI api = new PaperMythicRodAPI(
            "test-version",
            java.util.logging.Logger.getAnonymousLogger(),
            null, null, null, null, null
        );

        Result<PlatformItem> result = api.createRod("MYTHIC");

        assertFalse(result.isSuccess());
        assertTrue(result.getError().contains("MYTHIC"));
    }
}
