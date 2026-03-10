import re

with open('mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/fishing/FishingListener.java', 'r') as f:
    content = f.read()

content = content.replace('fishingService.processCatch(hookId, plugin.wrapPlayer(player), new io.xcutiboo.mythicrod.api.platform.PlatformLocation(hookLoc.getWorld().getName(), hookLoc.getX(), hookLoc.getY(), hookLoc.getZ(), hookLoc.getYaw(), hookLoc.getPitch()))', 'fishingService.processCatch(hookId, plugin.wrapPlayer(player))')
content = content.replace('effectsService.spawnExperienceEffects(player, xpAmount)', 'effectsService.spawnExperienceEffects(plugin.wrapPlayer(player), xpAmount)')
content = content.replace('rewardService.giveExperience(plugin.wrapPlayer(player), xpAmount, new io.xcutiboo.mythicrod.api.platform.PlatformLocation(hookLoc.getWorld().getName(), hookLoc.getX(), hookLoc.getY(), hookLoc.getZ(), hookLoc.getYaw(), hookLoc.getPitch()))', 'rewardService.giveExperience(plugin.wrapPlayer(player), xpAmount)')
content = content.replace('fishingService.cleanupStaleHooks(plugin.getServer())', 'fishingService.cleanupStaleHooks()')

with open('mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/fishing/FishingListener.java', 'w') as f:
    f.write(content)
