# Working examples

Snippets that compile against `mythicrod-api:2026.1.0`.

## VIP loot provider

```java
import io.xcutiboo.mythicrod.api.ExternalDropProvider;
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.api.platform.PlatformItem;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.paper.api.MythicRodServices;
import org.bukkit.plugin.java.JavaPlugin;

public final class VipLootPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        MythicRodServices.find().ifPresent(api ->
            api.registerExternalDropProvider(new VipReward(api)));
    }

    @Override
    public void onDisable() {
        MythicRodServices.find().ifPresent(api ->
            api.unregisterExternalDropProvider(VipReward.KEY));
    }
}

final class VipReward implements ExternalDropProvider {
    static final String KEY = "viploot:diamond";
    private final MythicRodAPI api;
    VipReward(MythicRodAPI api) { this.api = api; }

    @Override public String getKey() { return KEY; }
    @Override public String getTier() { return "rare"; }
    @Override public String getDisplayName() { return "<gold>VIP Diamond"; }

    @Override
    public double getWeight(PlatformPlayer player) {
        return player.hasPermission("viploot.vip") ? 4.0D : 0.0D;
    }

    @Override
    public PlatformItem generateItem(PlatformPlayer player) {
        return api.createItem("DIAMOND", 1).orElse(null);
    }
}
```

`paper-plugin.yml`:

```yaml
name: VipLoot
main: example.VipLootPlugin
api-version: '1.21'
dependencies:
  server:
    MythicRod: { load: AFTER, required: false }
```

## Refresh a leaderboard sign after each catch

```java
import io.xcutiboo.mythicrod.paper.events.MythicRodStatsUpdateEvent;
import io.xcutiboo.mythicrod.paper.api.MythicRodServices;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class LeaderboardListener implements Listener {
    private final SignRenderer renderer;
    public LeaderboardListener(SignRenderer renderer) { this.renderer = renderer; }

    @EventHandler
    public void onStats(MythicRodStatsUpdateEvent event) {
        MythicRodServices.find().ifPresent(api ->
            api.getTopPlayers(
                io.xcutiboo.mythicrod.api.PlayerStatSnapshot.StatType.TOTAL_CAUGHT, 10)
            .thenAccept(renderer::queueRefresh));
    }
}
```

`renderer::queueRefresh` must reschedule any Bukkit-touching work onto
the correct owner thread. See
[Folia threading](folia-threading.md).

## React to the language switch

```java
import io.xcutiboo.mythicrod.paper.events.MythicRodReloadEvent;

@EventHandler
public void onReload(MythicRodReloadEvent event) {
    if (event.isSuccess()) {
        myCachedTranslations.invalidate();
    }
}
```

The reload event fires both when MythicRod-side reload succeeds and
when it fails so listeners can clear stale data either way; the
`isSuccess()` flag lets you target the success path only if that is
what you need.

## Probe an item's MythicRod tier

```java
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

private static final NamespacedKey CUSTOM_ROD =
    NamespacedKey.fromString("mythicrod:custom_rod");
private static final NamespacedKey ROD_TIER =
    NamespacedKey.fromString("mythicrod:rod_tier");

public Optional<String> rodTier(ItemStack rod) {
    var pdc = rod.getItemMeta().getPersistentDataContainer();
    if (!pdc.has(CUSTOM_ROD, PersistentDataType.BYTE)) return Optional.empty();
    return Optional.ofNullable(pdc.get(ROD_TIER, PersistentDataType.STRING));
}
```

## Minigame: "what could I catch here?" preview

`previewEligibleDrops(UUID, biomeKey)` returns the drop table after
biome and permission filters. Use it in tutorial overlays, fishing
spot UIs, or scoreboard tickers without running an actual catch.

```java
import io.xcutiboo.mythicrod.api.MythicRodAPI;
import io.xcutiboo.mythicrod.api.platform.PlatformDrop;
import io.xcutiboo.mythicrod.paper.api.MythicRodServices;
import org.bukkit.entity.Player;

public void showPreview(Player player) {
    String biomeKey = player.getWorld()
        .getBiome(player.getLocation())
        .getKey()
        .toString();

    MythicRodServices.find().ifPresent(api -> {
        var eligible = api.previewEligibleDrops(player.getUniqueId(), biomeKey);
        int total = eligible.stream().mapToInt(PlatformDrop::getWeight).sum();
        eligible.forEach(drop -> {
            double share = total == 0 ? 0.0 : (drop.getWeight() * 100.0 / total);
            player.sendMessage("§7- " + drop.getIdentifier()
                + " §8(" + String.format("%.1f", share) + "%§8)");
        });
    });
}
```

Pass `null` as the biome to ignore biome filters. The list is an
immutable snapshot; reloads do not retroactively change a returned
list. Available since `2026.2.0`.

[← Developer API](../developer-api.md)
