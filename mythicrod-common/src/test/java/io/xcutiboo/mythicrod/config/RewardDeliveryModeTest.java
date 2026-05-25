package io.xcutiboo.mythicrod.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RewardDeliveryModeTest {

    @Test
    void configValuesAreStableLowercaseTokens() {
        assertEquals("vanilla_retrieve", RewardDeliveryMode.VANILLA_RETRIEVE.getConfigValue());
        assertEquals("inventory", RewardDeliveryMode.INVENTORY.getConfigValue());
        assertEquals("drop_at_player", RewardDeliveryMode.DROP_AT_PLAYER.getConfigValue());
    }

    @Test
    void nextCyclesAndWrapsAtEnd() {
        assertEquals(RewardDeliveryMode.INVENTORY, RewardDeliveryMode.VANILLA_RETRIEVE.next());
        assertEquals(RewardDeliveryMode.DROP_AT_PLAYER, RewardDeliveryMode.INVENTORY.next());
        assertEquals(RewardDeliveryMode.VANILLA_RETRIEVE, RewardDeliveryMode.DROP_AT_PLAYER.next());
    }

    @Test
    void previousCyclesAndWrapsAtStart() {
        assertEquals(RewardDeliveryMode.DROP_AT_PLAYER, RewardDeliveryMode.VANILLA_RETRIEVE.previous());
        assertEquals(RewardDeliveryMode.VANILLA_RETRIEVE, RewardDeliveryMode.INVENTORY.previous());
        assertEquals(RewardDeliveryMode.INVENTORY, RewardDeliveryMode.DROP_AT_PLAYER.previous());
    }

    @Test
    void fromConfigValueAcceptsCaseInsensitiveTrimmedInput() {
        assertEquals(RewardDeliveryMode.INVENTORY, RewardDeliveryMode.fromConfigValue("Inventory"));
        assertEquals(RewardDeliveryMode.DROP_AT_PLAYER, RewardDeliveryMode.fromConfigValue("  drop_at_player  "));
    }

    @Test
    void fromConfigValueReturnsNullForBlankOrUnknownInput() {
        assertNull(RewardDeliveryMode.fromConfigValue(null));
        assertNull(RewardDeliveryMode.fromConfigValue(""));
        assertNull(RewardDeliveryMode.fromConfigValue("   "));
        assertNull(RewardDeliveryMode.fromConfigValue("not_a_mode"));
    }
}
