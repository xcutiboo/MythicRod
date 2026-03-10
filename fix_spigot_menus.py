import os
import glob

spigot_menus = glob.glob("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/*.java")

for file in spigot_menus:
    with open(file, "r") as f:
        content = f.read()
    
    # Replace references to Player player = ... with Player p = getPlayer(); if needed, or fix missing variables
    # The common error is using 'player' instead of 'getPlayer()'
    # For instance, plugin.getGUIManager().openMenu(player, "config");
    
    content = content.replace("openMenu(player, ", "openMenu(getPlayer(), ")
    content = content.replace("openMainHub(player)", "openMainHub(getPlayer())")
    
    content = content.replace("plugin.audiences().player((org.bukkit.entity.Player) player).sendMessage(displayName);", 
                              "plugin.audiences().player(getPlayer()).sendMessage(displayName);")
                              
    content = content.replace("plugin.audiences().player((org.bukkit.entity.Player) player).sendMessage(component);",
                              "plugin.audiences().player(getPlayer()).sendMessage(component);")
                              
    content = content.replace("p.playSound(p.getLocation()", "getPlayer().playSound(getPlayer().getLocation()")
    
    content = content.replace("if (player.hasPermission", "if (getPlayer().hasPermission")
    content = content.replace("player.closeInventory()", "getPlayer().closeInventory()")
    content = content.replace("player.getUniqueId()", "getPlayer().getUniqueId()")

    with open(file, "w") as f:
        f.write(content)
        
print("Fixed menus")
