package io.xcutiboo.mythicrod.paper.update;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

import io.xcutiboo.mythicrod.paper.MythicRod;

/// Background poll against the GitHub releases REST API to compare the currently
/// installed plugin version against the latest published `v*` tag on
/// `xcutiboo/MythicRod`. Runs once on plugin enable and then every six hours.
/// Each poll is a single GET with a five-second connect timeout; no telemetry
/// is sent and no third-party service is contacted. Operators can disable the
/// check via `features.update-check.enabled: false` in `config.yml`.
public final class UpdateChecker {

    private static final String LATEST_RELEASE_URL =
        "https://api.github.com/repos/xcutiboo/MythicRod/releases/latest";
    private static final Pattern TAG_NAME = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final long INITIAL_DELAY_SECONDS = 30L;
    private static final long REPEAT_INTERVAL_SECONDS = 6L * 60L * 60L;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

    private final MythicRod plugin;
    private final Logger log;
    private final HttpClient http;
    private final AtomicReference<UpdateCheckTask> activeTask = new AtomicReference<>();
    private final String currentVersion;

    public UpdateChecker(MythicRod plugin) {
        this.plugin = plugin;
        this.log = plugin.getSLF4JLogger();
        this.currentVersion = plugin.getPluginMeta().getVersion();
        this.http = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public void start() {
        UpdateCheckTask task = new UpdateCheckTask();
        if (activeTask.compareAndSet(null, task)) {
            task.schedule();
        }
    }

    public void shutdown() {
        UpdateCheckTask task = activeTask.getAndSet(null);
        if (task != null) {
            task.cancel();
        }
    }

    private void runCheck() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(LATEST_RELEASE_URL))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MythicRod/" + currentVersion + " update-check")
                .timeout(HTTP_TIMEOUT)
                .GET()
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                Matcher m = TAG_NAME.matcher(resp.body());
                if (m.find()) {
                    String latest = m.group(1).replaceFirst("^v", "");
                    if (isNewerThanCurrent(latest)) {
                        log.warn("A newer MythicRod release is available: {} (you are running {}). "
                            + "See https://github.com/xcutiboo/MythicRod/releases/tag/v{}", latest, currentVersion, latest);
                    }
                }
            } else if (resp.statusCode() == 404) {
                log.debug("Update check: no published releases yet on GitHub.");
            } else {
                log.debug("Update check: GitHub returned HTTP {} for the latest-release endpoint.", resp.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException | IOException e) {
            log.debug("Update check failed: {}", e.getMessage());
        }
    }

    private boolean isNewerThanCurrent(String latest) {
        if (latest == null || latest.isBlank() || latest.equalsIgnoreCase(currentVersion)) {
            return false;
        }
        int[] cur = parse(currentVersion);
        int[] rem = parse(latest);
        if (cur == null || rem == null) {
            return !latest.equals(currentVersion);
        }
        for (int i = 0; i < cur.length; i++) {
            if (rem[i] > cur[i]) return true;
            if (rem[i] < cur[i]) return false;
        }
        return false;
    }

    private static int[] parse(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.find()) return null;
        return new int[] {
            Integer.parseInt(m.group(1)),
            Integer.parseInt(m.group(2)),
            Integer.parseInt(m.group(3)),
        };
    }

    private final class UpdateCheckTask {
        private BukkitTask bukkitTask;
        private volatile boolean cancelled;

        void schedule() {
            try {
                bukkitTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                    plugin,
                    () -> { if (!cancelled) runCheck(); },
                    INITIAL_DELAY_SECONDS * 20L,
                    REPEAT_INTERVAL_SECONDS * 20L);
            } catch (UnsupportedOperationException folia) {
                // Folia does not have a global Bukkit scheduler; fall back to the async scheduler.
                plugin.getServer().getAsyncScheduler().runAtFixedRate(
                    plugin,
                    task -> { if (!cancelled) runCheck(); },
                    INITIAL_DELAY_SECONDS, REPEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
            }
        }

        void cancel() {
            cancelled = true;
            if (bukkitTask != null) {
                try { bukkitTask.cancel(); } catch (RuntimeException _) { /* ignore */ }
            }
        }
    }
}
