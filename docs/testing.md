---
title: Testing checklist
nav_order: 9
---

# Testing checklist

A runbook for verifying a MythicRod install end-to-end on a fresh Paper
server. Follow top-to-bottom on a clean datapack so nothing leaks from a
previous run.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 0. Build & deploy

```bash
git clone https://github.com/xcutiboo/MythicRod.git
cd MythicRod
./gradlew :mythicrod-paper:shadowJar
cp mythicrod-paper/build/libs/MythicRod-Paper-*.jar /path/to/server/plugins/
```

Start the server. Look for the green banner in console:

```
MythicRod-Paper v26.5.0 | Paper 1.21.x | Brigadier ✓ | <Nms>ms
```

If the plugin disabled itself, copy the stack trace and head to
[Troubleshooting](./troubleshooting.md).

![divider]({{ site.baseurl }}/assets/divider.svg)

## 1. Permissions sanity

Run as **OP** in a test world:

| Command | Expected |
| --- | --- |
| `/mythicrod` | Opens the main GUI hub |
| `/mr help` | Lists every command the user can see |
| `/mythicrod debug` | Console dump of feature toggles, drop counts, locale |

Switch to a **non-OP** alt and re-run `/mr help`. Only base, GUI, stats, drop
view, and leaderboard entries should appear.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 2. Drop tables

1. `/mythicrod drops` — verify every category from `drops.yml` is listed.
2. `/mythicrod drops global` — confirm the first page of `global` drops.
3. `/mythicrod testroll 1000` — should print a tier histogram with five
   most-frequent identifiers and a "No-eligible" count.
4. `/mythicrod validate` — config health check; investigate every red ✗.
5. Cast a rod and catch fish 20+ times. Watch for varied catches, including
   tiered messages (common / uncommon / rare / legendary).

![divider]({{ site.baseurl }}/assets/divider.svg)

## 3. Biome filters

1. Stand in `minecraft:ocean`.
2. `/mythicrod testroll 500` — biome auto-detects from your location.
3. Catch a few fish; expect biome-scoped drops to surface (Nautilus Shell,
   Heart of the Sea, etc.).
4. Move to `minecraft:plains` and repeat. Ocean-only drops should drop out
   of the rotation.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 4. Permission gates

1. Set `features.permissions.enabled: true` in `config.yml`, reload.
2. Revoke `mythicrod.drops.legendary` on the test user.
3. `/mythicrod testroll 5000` — `legendary` count should hit zero.
4. Grant the node, repeat — legendary catches return.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 5. Custom rods

1. `/mythicrod rod` — visual + tier picker GUI opens.
2. `/mythicrod give <player> legendary` — target receives a Legendary rod.
3. `/mythicrod rod inspect` while holding it — console prints PDC keys
   (`mythicrod:rod_tier`, `mythicrod:rod_id`).
4. Catch with the legendary rod; rare/legendary tier rates should rise.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 6. Stats + leaderboard

1. `/mythicrod stats` — own snapshot opens.
2. `/mythicrod stats <other>` — admin can view (`stats.view.others` node).
3. `/mythicrod top 10` — leaderboard sorted by total catches.
4. `/mythicrod stats reset <player>` — wipes a row; rerun `/top` to confirm.

The default save cadence is 600 s; bump
`timers.stats-save-interval-seconds` to 30 in `config.yml`, reload, catch a
fish, wait, then check `plugins/MythicRod/statistics.yml` — your UUID block
should have a fresh `last_fished`.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 7. In-game drop editor

1. Open `/mythicrod drops <category>` while OP.
2. Click any drop — `EditDropMenu` opens.
3. Edit weight, amount, name, lore, glow, biomes, enchantments, flags,
   permission. Save. Confirm `drops.yml` reflects the change.
4. Add a new drop in the menu, save, restart the server, confirm it
   persisted.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 8. Configuration GUI

1. `/mythicrod gui` → Config.
2. Toggle Sounds / Particles / Statistics / Biome drops / Permissions /
   Debug — save and confirm `config.yml` updates.
3. Cycle `delivery_mode` between `vanilla_retrieve`, `inventory`, and
   `drop_at_player`. Re-cast; reward delivery should respect the new mode.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 9. Localization

1. `config.yml`: `language.default: ja_JP`. `/mythicrod reload`.
2. `/mr help` — entries render in Japanese.
3. `/mythicrod gui` → Language menu — per-player override picker opens.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 10. Reload safety

1. With players online and active stats, run `/mythicrod reload`.
2. Catches mid-reload should not crash; you'll see the reload banner in
   console.
3. Confirm `DropManager` published the new table (run `/mythicrod debug`).

![divider]({{ site.baseurl }}/assets/divider.svg)

## 11. API smoke test

Drop a tiny companion plugin onto the server with:

```java
@Override
public void onEnable() {
    MythicRodServices.find().ifPresent(api -> {
        getLogger().info("MythicRod version: " + api.getVersion());
        getLogger().info("Total drops: " + api.getDropCatalog().getTotalDropCount());
        api.registerExternalDropProvider(new ExternalDropProvider() {
            @Override public String getKey() { return "demo:gold"; }
            @Override public double getWeight(PlatformPlayer p) { return 0.5D; }
            @Override public PlatformItem generateItem(PlatformPlayer p) {
                return api.createItem("GOLD_INGOT", 1).orElse(null);
            }
            @Override public String getDisplayName() { return "<gold>Demo Gold</gold>"; }
            @Override public String getTier() { return "rare"; }
        });
    });
}
```

Catch a few fish — `demo:gold` should appear in catches.

Listen for `MythicRodRewardRollEvent` / `MythicRodFishCatchEvent` /
`MythicRodStatsUpdateEvent` to confirm the event pipeline fires.

![divider]({{ site.baseurl }}/assets/divider.svg)

## 12. Folia validation (when available)

If you have a Folia build, repeat sections 2, 5, and 11. Watch for thread
errors. MythicRod hands off to the target's region thread for inventory
mutation, so `Inventory#addItem` should never be called from the command
sender's thread.

---

[← Back to docs home](./) · [GitHub](https://github.com/xcutiboo/MythicRod) · [Hangar](https://hangar.papermc.io/xcutiboo/MythicRod)
