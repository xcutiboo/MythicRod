package io.xcutiboo.mythicrod.paper.effects;

import io.xcutiboo.mythicrod.MythicRod;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spawns and animates transient Display entities for fishing effects.
 *
 * <h2>Folia Safety</h2>
 * <ul>
 *   <li>Every {@link ScheduledTask} is obtained from {@link org.bukkit.entity.Entity#getScheduler()},
 *       which binds execution to the region that owns the entity's chunk — correct for Folia.</li>
 *   <li>{@code runAtFixedRate} / {@code runDelayed} may return {@code null} if the entity was
 *       already removed by the time the call executes. All return values are null-checked.</li>
 *   <li>Animation tasks are explicitly cancelled before the entity is removed so the retired
 *       callback path is never hit unexpectedly.</li>
 *   <li>{@link #animTaskById} uses an {@link AtomicInteger} key counter to avoid ID collision
 *       across concurrent spawns.</li>
 * </ul>
 */
public class DisplayEffectsService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Duration an ItemDisplay floats before removal (ticks). */
    private static final int FLOAT_DURATION_TICKS = 60;
    /** Half-amplitude of the bob oscillation in blocks. */
    private static final float BOB_AMPLITUDE = 0.25f;
    /** Scale applied to spawned ItemDisplays. */
    private static final float ITEM_SCALE = 1.5f;

    private final MythicRod plugin;

    /**
     * Thread-safe ID counter — prevents key collision if two displays spawn concurrently.
     */
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);

    /**
     * Maps animation-task ID → the running ScheduledTask so it can be cancelled
     * explicitly when the entity is removed via {@link #scheduleRemoval}.
     *
     * <p>Entries are removed either by the animation callback (entity died) or by
     * {@link #cancelAndRemove(int, Display)}.
     */
    private final Map<Integer, ScheduledTask> animTaskById = new ConcurrentHashMap<>();

    public DisplayEffectsService(MythicRod plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Spawns a floating, rotating ItemDisplay at {@code location} visible to {@code player}.
     * The entity removes itself after {@value #FLOAT_DURATION_TICKS} ticks.
     */
    public void spawnFloatingItem(Location location, ItemStack itemStack, Player player) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 0.5, 0);
        ItemDisplay display = location.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(itemStack.clone());
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
                    new Quaternionf()
            ));
        });

        int animId = animateFloatingItem(display);
        scheduleRemoval(animId, display, FLOAT_DURATION_TICKS);
    }

    /**
     * Spawns a billboard TextDisplay showing a MiniMessage-formatted catch announcement.
     * The display fades out after 3 seconds and is removed after 4 seconds.
     */
    public void spawnCatchAnnouncement(Location location, String message, Player player) {
        if (location.getWorld() == null) return;

        Component text = MINI.deserialize(message);
        Location spawnLoc = location.clone().add(0, 2.5, 0);

        TextDisplay display = location.getWorld().spawn(spawnLoc, TextDisplay.class, entity -> {
            entity.text(text);
            entity.setSeeThrough(true);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setBackgroundColor(Color.fromARGB(0x80, 0, 0, 0)); // semi-transparent black
            entity.setLineWidth(200);
            entity.setViewRange(50f);
        });

        // Fade out at 3 s, remove at 4 s — no animation task needed
        scheduleTextFade(display, 60L, 80L);
    }

    /**
     * Spawns a full legendary-catch effect: text overlay above the player and
     * a ring of celebration particles at {@code location}.
     */
    public void spawnLegendaryEffect(Location location, String itemName, Player player) {
        if (location.getWorld() == null) return;

        String gradientMessage = "<gradient:gold:yellow>✦ LEGENDARY CATCH ✦</gradient>\n"
                + "<gold>" + itemName + "</gold>\n"
                + "<gray>caught by " + player.getName() + "</gray>";

        spawnCatchAnnouncement(player.getLocation().clone().add(0, 3, 0), gradientMessage, player);
        spawnCelebrationParticles(location);
    }

    // =========================================================================
    // Animation helpers
    // =========================================================================

    /**
     * Schedules a per-tick bobbing + rotation animation for {@code display}.
     *
     * @return The animation-task ID, to pass to {@link #scheduleRemoval}.
     */
    private int animateFloatingItem(ItemDisplay display) {
        int id = taskIdCounter.getAndIncrement();

        // EntityScheduler.runAtFixedRate() returns null if the entity is already dead.
        ScheduledTask task = display.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (!display.isValid() || display.isDead()) {
                // Entity gone — cancel this task and clean up the map
                scheduledTask.cancel();
                animTaskById.remove(id);
                return;
            }

            // Bounded bob: sin wave between -BOB_AMPLITUDE and +BOB_AMPLITUDE
            float time       = (System.currentTimeMillis() % 4000L) / 4000f; // 0..1 over 4 s
            float bobY       = (float) (BOB_AMPLITUDE * Math.sin(2 * Math.PI * time));

            // Continuous Y-axis rotation
            float rotDeg     = (System.currentTimeMillis() % 4000L) / 4000f * 360f;
            AxisAngle4f aa   = new AxisAngle4f((float) Math.toRadians(rotDeg), 0, 1, 0);
            Quaternionf rot  = aa.get(new Quaternionf());

            Transformation current = display.getTransformation();
            display.setTransformation(new Transformation(
                    new Vector3f(current.getTranslation().x, bobY, current.getTranslation().z),
                    rot,
                    current.getScale(),
                    current.getRightRotation()
            ));
        }, null, 1L, 1L);

        if (task != null) {
            animTaskById.put(id, task);
        }
        return id;
    }

    /**
     * Schedules a two-phase fade for a TextDisplay:
     * 1. At {@code fadeTick}: set view range to 0 (invisible but still valid).
     * 2. At {@code removeTick}: remove the entity.
     */
    private void scheduleTextFade(TextDisplay display, long fadeTick, long removeTick) {
        // Fade phase — can return null if entity already dead; safe to ignore
        display.getScheduler().runDelayed(plugin, t -> {
            if (display.isValid()) {
                display.setViewRange(0f);
            }
        }, null, fadeTick);

        // Remove phase
        display.getScheduler().runDelayed(plugin, t -> {
            if (display.isValid() && !display.isDead()) {
                display.remove();
            }
        }, null, removeTick);
    }

    /**
     * Cancels the animation task with {@code animId} and schedules removal of
     * {@code entity} after {@code delayTicks}.
     *
     * <p>Cancelling the animation task first prevents the retired callback from
     * firing while the entity removal is in flight.
     */
    private void scheduleRemoval(int animId, Display entity, long delayTicks) {
        entity.getScheduler().runDelayed(plugin, t -> {
            // Cancel animation task before removing entity
            ScheduledTask animTask = animTaskById.remove(animId);
            if (animTask != null) {
                animTask.cancel();
            }
            if (entity.isValid() && !entity.isDead()) {
                entity.remove();
            }
        }, null, delayTicks);
    }

    // =========================================================================
    // Particle helpers
    // =========================================================================

    private void spawnCelebrationParticles(Location location) {
        if (location.getWorld() == null) return;

        // Ring of END_ROD particles around the catch location
        for (int i = 0; i < 20; i++) {
            double angle = 2 * Math.PI * i / 20;
            double x     = location.getX() + 1.5 * Math.cos(angle);
            double z     = location.getZ() + 1.5 * Math.sin(angle);
            location.getWorld().spawnParticle(
                    Particle.END_ROD,
                    x, location.getY() + 1, z,
                    3, 0.1, 0.1, 0.1, 0.05
            );
        }

        // Scattered TOTEM particles
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < 30; i++) {
            location.getWorld().spawnParticle(
                    Particle.TOTEM_OF_UNDYING,
                    location.clone().add(
                            rng.nextDouble(-2, 2),
                            rng.nextDouble(0, 3),
                            rng.nextDouble(-2, 2)),
                    1, 0, 0, 0, 0.1
            );
        }
    }
}
