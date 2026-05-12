package io.xcutiboo.mythicrod.drops;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.xcutiboo.mythicrod.api.platform.PlatformDrop;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import lombok.Getter;

@Getter
public class CustomDrop implements PlatformDrop {
    private final DropConfigurationRecord config;
    private final Map<String, Integer> enchantments = new ConcurrentHashMap<>();
    private final List<String> itemFlags;

    public CustomDrop(DropConfigurationRecord config) {
        this.config = config;
        this.enchantments.putAll(config.enchantments());
        this.itemFlags = new CopyOnWriteArrayList<>(config.itemFlags());
    }

    public static CustomDrop createNexoDrop(String nexoItemId, int weight, int amount) {
        return new CustomDrop(new DropConfigurationRecord(
            "nexo:" + nexoItemId, weight, amount, null, null, 0, null, null, false, null, null, nexoItemId
        ));
    }

    @Override
    public String getIdentifier() {
        return isNexoItem() ? "nexo:" + config.nexoItemId() : config.identifier();
    }

    @Override
    public int getWeight() {
        return config.weight();
    }

    @Override
    public int getAmount() {
        return config.amount();
    }

    public int getCustomModelData() {
        return config.customModelData();
    }

    public String getCustomName() {
        return config.customName();
    }

    public List<String> getLore() {
        return config.lore();
    }

    public Map<String, Integer> getEnchantments() {
        return Map.copyOf(enchantments);
    }

    public List<String> getItemFlags() {
        return List.copyOf(itemFlags);
    }

    public boolean isGlowing() {
        return config.glowing();
    }

    @Override
    public String getPermission() {
        return config.permission();
    }

    @Override
    public List<String> getBiomes() {
        return config.biomes();
    }

    public String getNexoItemId() {
        return config.nexoItemId();
    }

    @Override
    public boolean isNexoItem() {
        return config.nexoItemId() != null && !config.nexoItemId().isEmpty();
    }

    public String getTier() {
        int weight = getWeight();
        if (weight <= 1) {
            return "legendary";
        }
        if (weight <= 5) {
            return "rare";
        }
        if (weight <= 15) {
            return "uncommon";
        }
        return "common";
    }

    @Override
    public PlatformItem createItem() {
        throw new UnsupportedOperationException(
            "CustomDrop does not carry a platform item factory. Use MythicRodAPI#createItem or the reward item from MythicRodFishCatchEvent instead."
        );
    }

    public void addEnchantment(String enchantment, int level) {
        this.enchantments.put(enchantment, level);
    }

    public void addItemFlag(String flag) {
        this.itemFlags.add(flag);
    }
}
