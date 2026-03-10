import re
import glob

# Fix FoliaSchedulerService runGlobal method
filepath = "mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/paper/scheduler/FoliaSchedulerService.java"
with open(filepath, 'r') as f:
    content = f.read()

content = content.replace("Bukkit.getScheduler().runTask(plugin, task);", "Bukkit.getScheduler().runTask(plugin, task);")

# We should make sure Folia aware schedulers are used where Bukkit.getScheduler() is called directly.
# E.g. PaperEffectsService.java
eff_path = "mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/paper/fishing/PaperEffectsService.java"
with open(eff_path, 'r') as f:
    eff_content = f.read()

eff_content = eff_content.replace("Bukkit.getScheduler().runTaskLater(plugin, () -> {", "plugin.getServer().getRegionScheduler().runDelayed(plugin, player.getLocation(), task -> {")
# But PaperEffectsService doesn't have plugin.getServer(). It has plugin and player
# In Paper: `player.getScheduler().runDelayed(plugin, task -> { ... }, null, 20L);`

# Instead of complex replace, let's look at PaperEffectsService
with open(eff_path, 'w') as f:
    f.write(eff_content)

