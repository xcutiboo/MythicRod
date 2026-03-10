package io.xcutiboo.mythicrod.api.platform;

public class ItemContext {
    private final String name;
    private final java.util.List<String> lore;
    private final int customModelData;
    private final java.util.Map<String, Integer> enchantments;
    private final int amount;

    public ItemContext(String name, java.util.List<String> lore, int customModelData, java.util.Map<String, Integer> enchantments, int amount) {
        this.name = name;
        this.lore = lore;
        this.customModelData = customModelData;
        this.enchantments = enchantments;
        this.amount = amount;
    }

    public String getName() { return name; }
    public java.util.List<String> getLore() { return lore; }
    public int getCustomModelData() { return customModelData; }
    public java.util.Map<String, Integer> getEnchantments() { return enchantments; }
    public int getAmount() { return amount; }
}
