package io.xcutiboo.mythicrod.api.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class ItemProviderManager {
    private final Map<String, CustomItemProvider> providers = new HashMap<>();
    private final Logger logger;
    private boolean nexoWarned = false;

    public ItemProviderManager(Logger logger) {
        this.logger = logger;
    }

    public void registerProvider(String prefix, CustomItemProvider provider) {
        providers.put(prefix.toLowerCase(), provider);
    }

    public Optional<PlatformItem> buildItem(String fullId, ItemContext context) {
        String[] parts = fullId.split(":", 2);
        String prefix = parts.length > 1 ? parts[0].toLowerCase() : "minecraft";
        String id = parts.length > 1 ? parts[1] : fullId;

        CustomItemProvider provider = providers.get(prefix);
        if (provider != null) {
            Optional<PlatformItem> item = provider.buildItem(id, context);
            if (item.isEmpty() && prefix.equals("nexo") && !nexoWarned) {
                logger.warning("[MythicRod] Nexo item requested (" + fullId + ") but Nexo is not installed or item is invalid. Falling back gracefully. This warning will only show once.");
                nexoWarned = true;
            }
            return item;
        }

        // Fallback to vanilla
        CustomItemProvider vanilla = providers.get("minecraft");
        if (vanilla != null) {
            return vanilla.buildItem(id, context);
        }

        return Optional.empty();
    }
}
