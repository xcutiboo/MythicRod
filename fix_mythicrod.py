import re

with open('mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/MythicRod.java', 'r') as f:
    content = f.read()

# Add platformServer field and getPlatform method
if 'PlatformServer platformServer;' not in content:
    content = content.replace(
        'public final class MythicRod extends JavaPlugin implements MythicRodPlugin {',
        'public final class MythicRod extends JavaPlugin implements MythicRodPlugin {\n    private io.xcutiboo.mythicrod.api.platform.PlatformServer platformServer;\n    @Override\n    public io.xcutiboo.mythicrod.api.platform.PlatformServer getPlatform() { return platformServer; }\n'
    )

if 'this.platformServer = new io.xcutiboo.mythicrod.spigot.platform.SpigotServer(this);' not in content:
    content = content.replace(
        'this.api = injector.getInstance(MythicRodAPI.class);',
        'this.platformServer = new io.xcutiboo.mythicrod.spigot.platform.SpigotServer(this);\n            this.api = injector.getInstance(MythicRodAPI.class);'
    )

content = content.replace('dropManager.reload();', 'dropManager.loadDrops(configManager.getStatsConfig());')
content = content.replace('Metrics.SimplePie', 'org.bstats.charts.SimplePie')
content = content.replace('Metrics.SingleLineChart', 'org.bstats.charts.SingleLineChart')

with open('mythicrod-spigot/src/main/java/io/xcutiboo/mythicrod/MythicRod.java', 'w') as f:
    f.write(content)
