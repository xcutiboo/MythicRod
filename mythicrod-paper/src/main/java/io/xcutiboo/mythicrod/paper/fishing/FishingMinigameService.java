package io.xcutiboo.mythicrod.paper.fishing;

import io.xcutiboo.mythicrod.MythicRod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.Color;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class FishingMinigameService {
    
    private final MythicRod plugin;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final Map<UUID, MinigameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> activeTasks = new ConcurrentHashMap<>();
    
    private static final int GREEN_ZONE_MIN = 40;
    private static final int GREEN_ZONE_MAX = 60;
    private static final int SESSION_DURATION = 100;
    private static final int TENSION_TICKS = 3;
    
    public FishingMinigameService(MythicRod plugin) {
        this.plugin = plugin;
    }
    
    public void startMinigame(Player player, Location location, String rodTier, Runnable onSuccess, Runnable onFail) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            return;
        }
        
        MinigameSession session = new MinigameSession(player, location, rodTier, onSuccess, onFail);
        activeSessions.put(player.getUniqueId(), session);
        
        spawnTensionDisplay(session);
        startMinigameLoop(session);
    }
    
    private void spawnTensionDisplay(MinigameSession session) {
        Location displayLoc = session.getLocation().clone().add(0, 2.5, 0);
        
        TextDisplay display = session.getPlayer().getWorld().spawn(displayLoc, TextDisplay.class, entity -> {
            entity.text(buildTensionBar(0, false));
            entity.setSeeThrough(true);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setBackgroundColor(Color.fromARGB(0x80000000));
            entity.setViewRange(30f);
        });
        
        session.setDisplay(display);
    }
    
    private void startMinigameLoop(MinigameSession session) {
        TextDisplay display = session.getDisplay();
        if (display == null || !display.isValid()) {
            endMinigame(session, false);
            return;
        }
        
        ScheduledTask task = display.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!session.isActive()) {
                scheduledTask.cancel();
                activeTasks.remove(session.getPlayer().getUniqueId());
                endMinigame(session, false);
                return;
            }
            
            session.incrementTick();
            int tension = calculateTension(session);
            session.setTension(tension);
            
            boolean inGreenZone = tension >= GREEN_ZONE_MIN && tension <= GREEN_ZONE_MAX;
            updateTensionDisplay(session);
            
            if (inGreenZone && !session.hasPlayedGreenZoneSound()) {
                session.getPlayer().playSound(session.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                session.setPlayedGreenZoneSound(true);
            }
            
            if (session.getTick() >= SESSION_DURATION) {
                scheduledTask.cancel();
                activeTasks.remove(session.getPlayer().getUniqueId());
                endMinigame(session, false);
            }
        }, null, 1L, TENSION_TICKS);
        
        activeTasks.put(session.getPlayer().getUniqueId(), task);
    }
    
    private int calculateTension(MinigameSession session) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String rodTier = session.getRodTier();
        
        int baseTension = session.getTick() % 100;
        int difficultyMultiplier = switch (rodTier.toLowerCase(java.util.Locale.ROOT)) {
            case "basic" -> 1;
            case "advanced" -> 2;
            case "legendary" -> 3;
            default -> 1;
        };
        
        if (session.getTick() < 20) {
            return baseTension / (2 + difficultyMultiplier);
        } else if (session.getTick() > 80) {
            return Math.min(100, baseTension + random.nextInt(10, 20) * difficultyMultiplier);
        }
        
        int fluctuation = random.nextInt(-5, 6) * difficultyMultiplier;
        return Math.max(0, Math.min(100, baseTension + fluctuation));
    }
    
    private void updateTensionDisplay(MinigameSession session) {
        TextDisplay display = session.getDisplay();
        if (display == null || !display.isValid()) return;
        
        int tension = session.getTension();
        boolean inGreenZone = tension >= GREEN_ZONE_MIN && tension <= GREEN_ZONE_MAX;
        display.text(buildTensionBar(tension, inGreenZone));
    }
    
    private Component buildTensionBar(int tension, boolean inGreenZone) {
        StringBuilder bar = new StringBuilder();
        bar.append("<bold>");
        
        for (int i = 0; i < 100; i += 2) {
            if (i >= GREEN_ZONE_MIN && i <= GREEN_ZONE_MAX) {
                if (i <= tension) {
                    bar.append("<green>█</green>");
                } else {
                    bar.append("<dark_green>░</dark_green>");
                }
            } else {
                if (i <= tension) {
                    bar.append("<yellow>█</yellow>");
                } else {
                    bar.append("<gray>░</gray>");
                }
            }
        }
        
        bar.append("</bold>\n");
        
        if (inGreenZone) {
            bar.append("<green>⚡ PRESS NOW! ⚡</green>");
        } else if (tension < GREEN_ZONE_MIN) {
            bar.append("<yellow>Wait... Tension building...</yellow>");
        } else {
            bar.append("<red>Too high! Reel carefully!</red>");
        }
        
        return MINI_MESSAGE.deserialize(bar.toString());
    }
    
    public void playerClick(Player player) {
        MinigameSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !session.isActive()) return;
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastClick = currentTime - session.getLastClickTime();
        
        if (timeSinceLastClick < 50) {
            session.incrementRapidClickCount();
            if (session.getRapidClickCount() > 5) {
                session.setActive(false);
                player.sendMessage(Component.text("Anti-cheat: Clicking too fast!", net.kyori.adventure.text.format.NamedTextColor.RED));
                endMinigame(session, false);
                return;
            }
        } else {
            session.resetRapidClickCount();
        }
        
        session.setLastClickTime(currentTime);
        
        int tension = session.getTension();
        boolean inGreenZone = tension >= GREEN_ZONE_MIN && tension <= GREEN_ZONE_MAX;
        
        if (inGreenZone) {
            endMinigame(session, true);
        } else {
            session.setActive(false);
        }
    }
    
    private void endMinigame(MinigameSession session, boolean success) {
        session.setActive(false);
        activeSessions.remove(session.getPlayer().getUniqueId());
        
        ScheduledTask task = activeTasks.remove(session.getPlayer().getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        
        TextDisplay display = session.getDisplay();
        if (display != null && display.isValid()) {
            if (success) {
                display.text(MINI_MESSAGE.deserialize("<green><bold>✓ PERFECT CATCH!</bold></green>"));
                session.getPlayer().playSound(session.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            } else {
                display.text(MINI_MESSAGE.deserialize("<red><bold>✗ The fish escaped!</bold></red>"));
                session.getPlayer().playSound(session.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            
            display.getScheduler().runDelayed(plugin, scheduledTask -> {
                if (display.isValid()) {
                    display.remove();
                }
            }, null, 40L);
        }
        
        final Runnable callback = success ? session.getOnSuccess() : session.getOnFail();
        if (callback != null) {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, callback);
        }
    }
    
    public boolean isInMinigame(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
    
    public void cancelMinigame(Player player) {
        MinigameSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            session.setActive(false);
            
            ScheduledTask task = activeTasks.remove(player.getUniqueId());
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
            
            TextDisplay display = session.getDisplay();
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
    }
    
    private static class MinigameSession {
        private final Player player;
        private final Location location;
        private final String rodTier;
        private final Runnable onSuccess;
        private final Runnable onFail;
        private TextDisplay display;
        private int tick = 0;
        private int tension = 0;
        private volatile boolean active = true;
        private boolean playedGreenZoneSound = false;
        private long lastClickTime = 0;
        private int rapidClickCount = 0;
        
        public MinigameSession(Player player, Location location, String rodTier, Runnable onSuccess, Runnable onFail) {
            this.player = player;
            this.location = location;
            this.rodTier = rodTier;
            this.onSuccess = onSuccess;
            this.onFail = onFail;
        }
        
        public Player getPlayer() { return player; }
        public Location getLocation() { return location; }
        public String getRodTier() { return rodTier; }
        public Runnable getOnSuccess() { return onSuccess; }
        public Runnable getOnFail() { return onFail; }
        public TextDisplay getDisplay() { return display; }
        public void setDisplay(TextDisplay display) { this.display = display; }
        public int getTick() { return tick; }
        public void incrementTick() { this.tick++; }
        public int getTension() { return tension; }
        public void setTension(int tension) { this.tension = tension; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public boolean hasPlayedGreenZoneSound() { return playedGreenZoneSound; }
        public void setPlayedGreenZoneSound(boolean played) { this.playedGreenZoneSound = played; }
        public long getLastClickTime() { return lastClickTime; }
        public void setLastClickTime(long time) { this.lastClickTime = time; }
        public int getRapidClickCount() { return rapidClickCount; }
        public void incrementRapidClickCount() { this.rapidClickCount++; }
        public void resetRapidClickCount() { this.rapidClickCount = 0; }
    }
}
