package io.xcutiboo.mythicrod.fishing;

import java.util.Map;
import java.util.logging.Logger;

import io.xcutiboo.mythicrod.api.Result;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformItemFactory;
import io.xcutiboo.mythicrod.api.platform.PlatformLocation;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.api.platform.PlatformWorld;
import io.xcutiboo.mythicrod.drops.CustomDrop;
import io.xcutiboo.mythicrod.config.LanguageManager;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RewardService {
    private final PlatformServer platformServer;
    private final LanguageManager languageManager;
    @NonNull
    private final Logger logger;

    public PlatformItem createItemStack(CustomDrop drop) {
        if (drop == null) {
            return null;
        }
        
        try {
            // Use PlatformItemFactory instead of drop.createItem() which returns null
            PlatformItemFactory factory = platformServer.getItemFactory();
            if (factory == null) {
                logger.warning("ItemFactory not available");
                return null;
            }
            
            Result<PlatformItem> result = factory.createItem(drop.getIdentifier(), drop.getAmount());
            if (result.isSuccess()) {
                return result.getValue();
            } else {
                logger.warning("Failed to create item for drop " + drop.getIdentifier() + ": " + result.getError());
                return null;
            }
        } catch (Exception e) {
            logger.warning("Failed to create item from drop " + drop.getIdentifier() + ": " + e.getMessage());
            return null;
        }
    }
    
    public boolean deliverReward(PlatformPlayer player, CustomDrop drop, PlatformLocation location) {
        try {
            PlatformItem customItem = createItemStack(drop);
            if (customItem == null) {
                return false;
            }

            Map<Integer, PlatformItem> leftover = player.getInventory().addItem(customItem);
            if (!leftover.isEmpty()) {
                PlatformWorld world = platformServer.getWorld(location.getWorldName());
                if (world != null) {
                    for (PlatformItem remaining : leftover.values()) {
                        world.dropItem(location, remaining);
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            logger.warning("Failed to deliver reward to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public String getCatchMessage(CustomDrop drop) {
        if (drop == null) return "";

        String itemName = drop.getCustomName() != null ?
            drop.getCustomName() :
            formatMaterialName(drop.getIdentifier());

        String messageKey = determineMessageKey(drop.getChance());
        
        return languageManager.tr(messageKey,
            Map.of("item", itemName, "amount", String.valueOf(drop.getAmount())));
    }

    public int calculateExperience(CustomDrop drop) {
        double chance = drop.getChance();
        if (chance <= 1) return 6;   // Legendary
        if (chance <= 5) return 5;   // Rare
        if (chance <= 15) return 3;  // Uncommon
        if (chance <= 30) return 2;  // Common
        return 1;                     // Very common
    }

    private String determineMessageKey(double chance) {
        if (chance <= 1) return "fishing.catch-legendary";
        if (chance <= 5) return "fishing.catch-rare";
        return "fishing.catch-normal";
    }

    private String formatMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return "Unknown";
        }
        String formatted = materialName.replace("_", " ").toLowerCase(java.util.Locale.ROOT);
        if (formatted.startsWith("nexo:")) {
            formatted = formatted.substring(5);
        }
        return formatted.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + formatted.substring(1);
    }
}
