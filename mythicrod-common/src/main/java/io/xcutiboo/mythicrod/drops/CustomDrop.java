package io.xcutiboo.mythicrod.drops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.xcutiboo.mythicrod.api.platform.PlatformDrop;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import lombok.Getter;

@Getter
public class CustomDrop implements PlatformDrop {
    private final DropConfigurationRecord config;
    private final Map<String, Integer> enchantments = new HashMap<>();
    private final List<String> itemFlags;

    public CustomDrop(DropConfigurationRecord config) {
        this.config = config;
        this.enchantments.putAll(config.enchantments());
        this.itemFlags = new ArrayList<>(config.itemFlags());
    }

    public static CustomDrop createNexoDrop(String nexoItemId, int chance, int amount) {
        return new CustomDrop(new DropConfigurationRecord(
            "nexo:" + nexoItemId, chance, amount, null, null, 0, null, null, false, null, null, nexoItemId
        ));
    }

    @Override
    public String getIdentifier() { 
        return isNexoItem() ? "nexo:" + config.nexoItemId() : config.identifier(); 
    }
    
    @Override
    public int getChance() { 
        return config.chance(); 
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

    @Override
    public PlatformItem createItem() { return null; }

    public void addEnchantment(String enchantment, int level) {
        this.enchantments.put(enchantment, level);
    }
    
    public void addItemFlag(String flag) {
        this.itemFlags.add(flag);
    }
}
