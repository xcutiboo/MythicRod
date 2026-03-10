import os
import glob
import re

def process_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Reset any changes that might have messed up the 'player' variable
    # We will just declare a Player object inside the methods to make sure the Bukkit Player is available where needed
    
    if "LanguageSwitchMenu.java" in filepath:
        # Instead of replacing trForSender params, we'll cast the existing Player objects
        # First we need to make sure we have a Player object where needed
        content = re.sub(r'plugin\.getLanguageManager\(\)\.trForSender\((p|player|getPlayer\(\)),', r'plugin.getLanguageManager().trForSender(plugin.wrapPlayer(\1),', content)
        content = re.sub(r'plugin\.audiences\(\)\.player\(player\)', r'plugin.audiences().player((org.bukkit.entity.Player) player)', content)
        content = re.sub(r'plugin\.audiences\(\)\.player\(p\)', r'plugin.audiences().player((org.bukkit.entity.Player) p)', content)
        
        # But wait, 'player' is not defined in most methods in LanguageSwitchMenu except the constructor. 
        # Most methods use 'getPlayer()' from BaseMenu.
        # Let's fix the undefined 'player' and 'p' issues. 
        # If the file has 'player' undefined, we replace it with 'getPlayer()'
        # Except inside switchLanguage where 'p' is defined, and constructor where 'player' is defined.
        
        # The easiest way is to add `Player player = getPlayer();` at the beginning of build()
        if "Player player = getPlayer();" not in content and "protected void build() {" in content:
            content = content.replace("protected void build() {", "protected void build() {\n        Player player = getPlayer();")
            
        if "private void sendMessage(String message) {" in content and "Player player = getPlayer();" not in content:
            content = content.replace("private void sendMessage(String message) {", "private void sendMessage(String message) {\n        Player player = getPlayer();")

        if "private String formatLanguageName(String code) {" in content and "Player player = getPlayer();" not in content:
            content = content.replace("private String formatLanguageName(String code) {", "private String formatLanguageName(String code) {\n        Player player = getPlayer();")
            
    if "MainHubMenu.java" in filepath:
        if "Player player = getPlayer();" not in content and "protected void build() {" in content:
            content = content.replace("protected void build() {", "protected void build() {\n        Player player = getPlayer();")
        if "private void sendMessage(String message) {" in content and "Player player = getPlayer();" not in content:
            content = content.replace("private void sendMessage(String message) {", "private void sendMessage(String message) {\n        Player player = getPlayer();")
            
    if "StatsMenu.java" in filepath:
        if "Player player = getPlayer();" not in content and "private void buildPersonalStats() {" in content:
            content = content.replace("private void buildPersonalStats() {", "private void buildPersonalStats() {\n        Player player = getPlayer();")
        if "Player player = getPlayer();" not in content and "private void buildLeaderboard() {" in content:
            content = content.replace("private void buildLeaderboard() {", "private void buildLeaderboard() {\n        Player player = getPlayer();")

    with open(filepath, 'w') as f:
        f.write(content)

for filepath in glob.glob("mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/spigot/gui/menus/*.java"):
    process_file(filepath)

