package io.xcutiboo.mythicrod.api.platform;

import java.util.List;
import java.util.Map;

/// Immutable, inspection-oriented view of an item understood by MythicRod.
///
/// External integrations receive and return this type instead of concrete Paper
/// classes so the public API stays stable and easy to test.
public interface PlatformItem {

    /// Returns the stable identifier for this item.
    ///
    /// @return stable identifier such as `DIAMOND` or `nexo:my_item`
    String getIdentifier();

    /// Returns the stack amount represented by this view.
    ///
    /// @return stack amount represented by this item view
    int getAmount();

    /// Returns the display name visible to players.
    ///
    /// @return display name shown to players, or a platform default when unset
    String getDisplayName();

    /// Returns the lore lines in display order.
    ///
    /// @return immutable snapshot of lore lines in display order
    List<String> getLore();

    /// Returns the applied enchantments keyed by normalized identifier.
    ///
    /// @return immutable snapshot of enchantments keyed by their normalized identifier
    Map<String, Integer> getEnchantments();

    /// Returns the item flags applied to this item.
    ///
    /// @return immutable snapshot of item flags applied to this item
    List<String> getItemFlags();

    /// Returns whether this item should render with a glow effect.
    ///
    /// @return `true` when the item should render with an enchantment glow
    boolean isGlowing();

    /// Returns whether this item came from a custom-item integration.
    ///
    /// @return `true` when the item came from a custom-item integration rather than vanilla materials
    boolean isCustom();
}
