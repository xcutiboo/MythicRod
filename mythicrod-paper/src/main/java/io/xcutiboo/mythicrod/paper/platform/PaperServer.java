package io.xcutiboo.mythicrod.paper.platform;

import java.io.File;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import io.xcutiboo.mythicrod.api.platform.PlatformCommandSender;
import io.xcutiboo.mythicrod.api.platform.PlatformConfiguration;
import io.xcutiboo.mythicrod.api.platform.PlatformItemFactory;
import io.xcutiboo.mythicrod.api.platform.PlatformPlayer;
import io.xcutiboo.mythicrod.api.platform.PlatformScheduler;
import io.xcutiboo.mythicrod.api.platform.PlatformServer;
import io.xcutiboo.mythicrod.api.platform.PlatformWorld;
import io.xcutiboo.mythicrod.paper.config.PaperConfiguration;
import io.xcutiboo.mythicrod.paper.item.ItemFactory;
import io.xcutiboo.mythicrod.paper.scheduler.FoliaSchedulerService;
import io.xcutiboo.mythicrod.text.ConfiguredText;

public class PaperServer implements PlatformServer {
    private final Server server;
    private final PlatformScheduler scheduler;
    private final PlatformItemFactory itemFactory;

    public PaperServer(Server server, Plugin plugin) {
        this.server = server;
        this.scheduler = new FoliaSchedulerService(plugin);
        this.itemFactory = new ItemFactory(server.getLogger());
    }

    @Override
    public Logger getLogger() {
        return server.getLogger();
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public PlatformPlayer getPlayer(UUID uuid) {
        Player player = server.getPlayer(uuid);
        return player != null ? new PaperPlayer(player) : null;
    }

    @Override
    public PlatformCommandSender getCommandSender(String name) {
        Player player = server.getPlayer(name);
        if (player != null) {
            return new PaperPlayer(player);
        }
        return new PaperCommandSender(server.getConsoleSender());
    }

    @Override
    public boolean isEntityValid(UUID entityId) {
        return server.getEntity(entityId) != null;
    }

    @Override
    public boolean isNexoEnabled() {
        return server.getPluginManager().isPluginEnabled("Nexo");
    }

    @Override
    public PlatformConfiguration loadConfiguration(File file) {
        return new PaperConfiguration(file);
    }

    @Override
    public PlatformConfiguration loadConfiguration(InputStream stream) {
        return new PaperConfiguration(stream);
    }

    @Override
    public PlatformConfiguration createEmptyConfiguration() {
        return new PaperConfiguration();
    }

    @Override
    public void dispatchCommandConsole(String command) {
        server.dispatchCommand(server.getConsoleSender(), command);
    }

    @Override
    public void broadcastMessage(String message) {
        server.broadcast(ConfiguredText.parse(message));
    }

    @Override
    public PlatformWorld getWorld(String name) {
        World world = server.getWorld(name);
        return world != null ? new PaperWorld(world) : null;
    }

    @Override
    public PlatformItemFactory getItemFactory() {
        return itemFactory;
    }

    public Server getBukkitServer() {
        return server;
    }
}
