package io.xcutiboo.mythicrod.config;

import java.util.Locale;

public enum RewardDeliveryMode {
    VANILLA_RETRIEVE("vanilla_retrieve"),
    INVENTORY("inventory"),
    DROP_AT_PLAYER("drop_at_player");

    private final String configValue;

    RewardDeliveryMode(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigValue() {
        return configValue;
    }

    public RewardDeliveryMode next() {
        RewardDeliveryMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    public RewardDeliveryMode previous() {
        RewardDeliveryMode[] modes = values();
        return modes[(ordinal() + modes.length - 1) % modes.length];
    }

    public static RewardDeliveryMode fromConfigValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalizedValue = rawValue.trim().toLowerCase(Locale.ROOT);
        for (RewardDeliveryMode mode : values()) {
            if (mode.configValue.equals(normalizedValue)) {
                return mode;
            }
        }

        return null;
    }
}
