package io.xcutiboo.mythicrod.fishing;

import java.util.Map;

import io.xcutiboo.mythicrod.MythicRodPlugin;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformLocation;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class RewardService {
    private final MythicRodPlugin plugin;

    public RewardService(MythicRodPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean deliverReward(PlatformPlayer player, CustomDrop drop, PlatformLocation location) {
        try {
            PlatformItem customItem = drop.createItem();
            if (customItem == null) {
                return false;
            }

            // In a real implementation, we'd check player inventory here
            // using PlatformInventory abstraction. For now, we delegate to platform.
            
            // To be implemented fully via PlatformInventory
            // if (player.getInventory().isFull()) {
            //    plugin.getPlatform().getWorld(location.getWorldName()).dropItem(location, customItem);
            // } else {
            //    player.getInventory().addItem(customItem);
            // }
            
            // For now, we'll assume the platform handles this internally via an event or service
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give item to player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public void sendCatchMessage(PlatformPlayer player, CustomDrop drop) {
        if (player == null || !player.isOnline() || drop == null) return;

        try {
            String itemName = drop.getCustomName() != null ?
                drop.getCustomName() :
                formatMaterialName(drop.getIdentifier());

            String messageKey = determineMessageKey(drop.getChance());

            Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getLanguageManager().tr(messageKey,
                    Map.of("item", itemName, "amount", String.valueOf(drop.getAmount())))
            );

            plugin.sendFormattedMessage(player, LegacyComponentSerializer.legacyAmpersand().serialize(message));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send catch message: " + e.getMessage());
        }
    }

    public int calculateExperience(CustomDrop drop) {
        double chance = drop.getChance();
        if (chance <= 1) return 6;
        if (chance <= 5) return 5;
        if (chance <= 15) return 3;
        if (chance <= 30) return 2;
        return 1;
    }

    // Experience giving will be handled by the platform-specific listener directly
    // since experience is highly tied to Bukkit Entity/Player objects and not abstracted easily

    private String determineMessageKey(double chance) {
        if (chance <= 1) return "fishing.catch-legendary";
        if (chance <= 5) return "fishing.catch-rare";
        return "fishing.catch-normal";
    }

    private String formatMaterialName(String materialName) {
        String formatted = materialName.replace("_", " ").toLowerCase();
        if (formatted.startsWith("nexo:")) {
            formatted = formatted.substring(5);
        }
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }
}
