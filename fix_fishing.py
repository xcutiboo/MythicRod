import os

file_path = "mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/fishing/FishingListener.java"
with open(file_path, "r") as f:
    content = f.read()

# Fix processCatch call
content = content.replace("FishingResult result = fishingService.processCatch(hookId, plugin.wrapPlayer(player));",
                          "FishingResult result = fishingService.processCatch(hookId, plugin.wrapPlayer(player), new io.xcutiboo.mythicrod.api.platform.PlatformLocation(hook.getLocation().getWorld().getName(), hook.getLocation().getX(), hook.getLocation().getY(), hook.getLocation().getZ(), hook.getLocation().getYaw(), hook.getLocation().getPitch()), hook.getLocation().getBlock().getBiome().name());")

# Fix giveExperience by changing to player.giveExp
content = content.replace("rewardService.giveExperience(plugin.wrapPlayer(player), xpAmount);",
                          "player.giveExp(xpAmount);")

# Fix cleanupStaleHooks
content = content.replace("fishingService.cleanupStaleHooks();",
                          "fishingService.cleanupStaleHooks(plugin.getPlatform());")

with open(file_path, "w") as f:
    f.write(content)

print("Fixed FishingListener.java")
