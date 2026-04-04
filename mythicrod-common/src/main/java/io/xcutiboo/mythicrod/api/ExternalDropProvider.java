package io.xcutiboo.mythicrod.api;

import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extension point for external plugins to inject custom drops into MythicRod's
 * drop selection pipeline via the platform-agnostic common API.
 *
 * <p>Register implementations via {@link MythicRodAPI#registerExternalDropProvider}.
 * Each registered provider is consulted during drop selection.
 *
 * <p><strong>Thread Safety:</strong> All methods may be called from entity region
 * threads (Folia). Implementations MUST be thread-safe and must NOT block.
 *
 * <p>For Paper-specific implementations using {@code ItemStack} directly,
 * use the {@code PaperExternalDropProvider} interface in the paper module.
 */
public interface ExternalDropProvider {

    /**
     * Returns the unique string key identifying this drop provider.
     * Format: {@code "namespace:key"} (e.g. {@code "myplugin:vip_reward"}).
     * Must be stable across restarts.
     *
     * @return Non-null, non-empty unique key string.
     */
    @NotNull
    String getKey();

    /**
     * Returns the drop weight for this provider given the current player context.
     * A weight of {@code 0.0} or less means this provider will never be selected.
     *
     * <p>Weight is relative to all other providers and built-in drops.
     *
     * @param player The fishing player context (read-only access).
     * @return Weight &ge; 0.0. Negative values are treated as {@code 0.0}.
     */
    double getWeight(@NotNull PlatformPlayer player);

    /**
     * Generates the platform item reward for this drop provider.
     *
     * <p>Called only if this provider is selected by the weighted random roll.
     * Return {@code null} to silently abort the drop.
     *
     * @param player The player who triggered the catch.
     * @return The reward item, or {@code null} to skip.
     */
    @Nullable
    PlatformItem generateItem(@NotNull PlatformPlayer player);

    /**
     * Display name shown in GUI and chat notifications (MiniMessage format).
     * Default: {@code "<gray>Unknown Drop</gray>"}
     *
     * @return MiniMessage-formatted display name.
     */
    @NotNull
    default String getDisplayName() {
        return "<gray>Unknown Drop</gray>";
    }

    /**
     * Rarity tier string for sorting and statistics.
     * Standard values: {@code "common"}, {@code "uncommon"}, {@code "rare"}, {@code "legendary"}.
     * Default: {@code "common"}
     *
     * @return Lowercase rarity string.
     */
    @NotNull
    default String getTier() {
        return "common";
    }
}
