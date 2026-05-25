package io.xcutiboo.mythicrod.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void successCarriesValueAndReportsSuccess() {
        Result<String> result = Result.success("ok");
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("ok", result.getValue());
        assertNull(result.getError());
    }

    @Test
    void failureCarriesErrorAndReportsFailure() {
        Result<String> result = Result.failure("boom");
        assertFalse(result.isSuccess());
        assertTrue(result.isFailure());
        assertNull(result.getValue());
        assertEquals("boom", result.getError());
    }

    @Test
    void orElseReturnsValueWhenPresentAndFallbackOtherwise() {
        assertEquals("hit", Result.success("hit").orElse("miss"));
        assertEquals("fallback", Result.<String>failure("err").orElse("fallback"));
    }

    @Test
    void orElseThrowReturnsValueWhenPresent() {
        assertEquals("ok", Result.success("ok").orElseThrow());
    }

    @Test
    void orElseThrowRaisesWithStoredErrorMessage() {
        Result<String> result = Result.failure("explicit");
        IllegalStateException thrown = assertThrows(IllegalStateException.class, result::orElseThrow);
        assertEquals("explicit", thrown.getMessage());
    }

    @Test
    void orElseThrowFallsBackToGenericMessageWhenErrorBlank() {
        Result<String> result = Result.failure("   ");
        IllegalStateException thrown = assertThrows(IllegalStateException.class, result::orElseThrow);
        assertEquals("Operation failed without an error message", thrown.getMessage());
    }
}
