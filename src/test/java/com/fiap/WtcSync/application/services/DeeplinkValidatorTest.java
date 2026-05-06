package com.fiap.WtcSync.application.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeeplinkValidatorTest {

    private final DeeplinkValidator validator = new DeeplinkValidator();

    @Test
    void validateDeeplink_validHomeRoute_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink("wtcapp://home"));
    }

    @Test
    void validateDeeplink_validProfileRoute_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink("wtcapp://profile/cust-123"));
    }

    @Test
    void validateDeeplink_validEventsRoute_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink("wtcapp://events/evt-456"));
    }

    @Test
    void validateDeeplink_validCampaignsRoute_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink("wtcapp://campaigns/camp-789"));
    }

    @Test
    void validateDeeplink_validInboxRoute_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink("wtcapp://inbox"));
    }

    @Test
    void validateDeeplink_validPurchaseRoute_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink("wtcapp://purchase/pur-001"));
    }

    @Test
    void validateDeeplink_nullDeeplink_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink(null));
    }

    @Test
    void validateDeeplink_blankDeeplink_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink("   "));
    }

    @Test
    void validateDeeplink_externalHttpUrl_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink("https://wtc.com/evento"));
    }

    @Test
    void validateDeeplink_externalHttpsUrl_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateDeeplink("http://wtc.com/evento"));
    }

    @Test
    void validateDeeplink_unknownWtcappRoute_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateDeeplink("wtcapp://unknown/path"));
        assertTrue(ex.getMessage().contains("Deeplink inválido"));
        assertTrue(ex.getMessage().contains("wtcapp://unknown/path"));
    }

    @Test
    void validateDeeplink_wtcappWithExtraSegments_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateDeeplink("wtcapp://profile/123/extra"));
    }

    @Test
    void validateDeeplink_profileWithEmptyId_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateDeeplink("wtcapp://profile/"));
    }

    @Test
    void validateActionUrls_validUrls_shouldNotThrow() {
        Map<String, String> urls = Map.of(
                "btn1", "wtcapp://events/evt-1",
                "btn2", "https://wtc.com/promo"
        );
        assertDoesNotThrow(() -> validator.validateActionUrls(urls));
    }

    @Test
    void validateActionUrls_nullMap_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateActionUrls(null));
    }

    @Test
    void validateActionUrls_emptyMap_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateActionUrls(Map.of()));
    }

    @Test
    void validateActionUrls_invalidDeeplink_shouldThrow() {
        Map<String, String> urls = Map.of("btn1", "wtcapp://invalid/route");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validateActionUrls(urls));
        assertTrue(ex.getMessage().contains("wtcapp://invalid/route"));
    }

    @Test
    void getRoutes_shouldReturnAllValidRoutes() {
        var routes = validator.getRoutes();
        assertEquals(6, routes.size());
        assertTrue(routes.stream().anyMatch(r -> r.pattern().equals("wtcapp://home")));
        assertTrue(routes.stream().anyMatch(r -> r.pattern().equals("wtcapp://profile/{customerId}")));
        assertTrue(routes.stream().anyMatch(r -> r.pattern().equals("wtcapp://events/{eventId}")));
        assertTrue(routes.stream().anyMatch(r -> r.pattern().equals("wtcapp://campaigns/{campaignId}")));
        assertTrue(routes.stream().anyMatch(r -> r.pattern().equals("wtcapp://inbox")));
        assertTrue(routes.stream().anyMatch(r -> r.pattern().equals("wtcapp://purchase/{purchaseId}")));
    }
}
