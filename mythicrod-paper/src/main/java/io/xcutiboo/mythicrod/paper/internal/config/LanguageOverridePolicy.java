package io.xcutiboo.mythicrod.paper.internal.config;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LanguageOverridePolicy {
    private static final Set<String> REFRESHABLE_GUI_TITLE_KEYS = Set.of(
        "gui.main.title",
        "gui.language.title",
        "gui.drops.title",
        "gui.drops.category_title",
        "gui.config.title",
        "gui.stats.title",
        "gui.stats.leaderboard_title",
        "gui.edit_drop.title",
        "gui.rod.title"
    );
    private static final Set<String> SHIPPED_BLACK_GUI_TITLE_DEFAULTS = Set.of(
        "<black>✦ MythicRod",
        "<black>🌐 Language Selection",
        "<black>🌐 言語選択",
        "<black>🎣 Fishing Drops",
        "<black>🎣 釣りドロップ",
        "<black>🎣 %category% Drops",
        "<black>🎣 %category% ドロップ",
        "<black>⚙ Configuration",
        "<black>⚙ 設定",
        "<black>📊 Your Statistics",
        "<black>📊 あなたの統計",
        "<black>🏆 Top Fishers",
        "<black>🏆 トップ釣り師",
        "<black>✏ Edit Drop: %identifier%",
        "<black>✏ ドロップ編集: %identifier%",
        "<black>🎣 MythicRod Menu",
        "<black>🎣 MythicRodメニュー"
    );
    private static final Map<String, Map<String, String>> EXACT_REFRESH_REPLACEMENTS = Map.ofEntries(
        Map.entry("drops.category-header", Map.of(
            "<gold><st>══</st><bold> Drops: <yellow>%category% </bold><gold><st>══</st>",
            "<gold><st>══</st><bold> Drops: <yellow>%label% </bold><gold><st>══</st>",
            "<gold><st>══</st><bold> ドロップ: <yellow>%category% </bold><gold><st>══</st>",
            "<gold><st>══</st><bold> ドロップ: <yellow>%label% </bold><gold><st>══</st>"
        )),
        Map.entry("drops.category-entry", Map.of(
            "<yellow>  %category%<gray>: <white>%count% drops",
            "<yellow>  %label% <dark_gray>(%category%)<gray>: <white>%count% drops",
            "<yellow>  %category%<gray>: <white>%count% ドロップ",
            "<yellow>  %label% <dark_gray>(%category%)<gray>: <white>%count% ドロップ"
        )),
        Map.entry("gui.stats.title", Map.of(
            "<black>📊 Fishing Statistics",
            "<gold><bold>MythicRod</bold> <dark_gray>• <green>Stats",
            "<black>📊 釣り統計",
            "<gold><bold>MythicRod</bold> <dark_gray>• <green>Stats"
        ))
    );
    private static final Map<String, Set<String>> BUNDLED_VALUE_REFRESHES = Map.of(
        "drops.drop-entry", Set.of(
            "<dark_gray>  • <white>%name% <gray>(<yellow>%chance%%<gray> · <yellow>×%amount%<gray>)",
            "<dark_gray>  • <white>%name% <gray>(weight <yellow>%chance%<gray> · <yellow>×%amount%<gray>)",
            "<dark_gray>  • <white>%name% <gray>(ウェイト <yellow>%chance%<gray> · <yellow>×%amount%<gray>)"
        ),
        "gui.edit_drop.weight.left_click", Set.of(
            "<yellow>  L-Click: <gray>+1",
            "<yellow>  左クリック: <gray>+1"
        ),
        "gui.edit_drop.weight.right_click", Set.of(
            "<yellow>  R-Click: <gray>-1",
            "<yellow>  右クリック: <gray>-1"
        ),
        "gui.edit_drop.amount.left_click", Set.of(
            "<yellow>  L-Click: <gray>+1",
            "<yellow>  左クリック: <gray>+1"
        ),
        "gui.edit_drop.amount.right_click", Set.of(
            "<yellow>  R-Click: <gray>-1",
            "<yellow>  右クリック: <gray>-1"
        ),
        "gui.edit_drop.amount.shift_click", Set.of(
            "<gold>  Shift: <gray>±10"
        )
    );

    private LanguageOverridePolicy() {
    }

    public static boolean shouldUseDiskOverride(String key, String diskValue, String bundledValue) {
        if (key == null || diskValue == null) {
            return true;
        }
        if (bundledValue != null && bundledValue.equals(diskValue)) {
            return true;
        }
        if (bundledValue == null) {
            return false;
        }

        return replacementForDiskValue(key, diskValue, bundledValue).isEmpty();
    }

    public static boolean shouldRefreshDiskValue(String key, String diskValue) {
        if (key == null || diskValue == null) {
            return false;
        }

        String trimmedValue = diskValue.trim();
        if (EXACT_REFRESH_REPLACEMENTS.getOrDefault(key, Map.of()).get(trimmedValue) != null) {
            return true;
        }
        if (BUNDLED_VALUE_REFRESHES.getOrDefault(key, Set.of()).contains(trimmedValue)) {
            return true;
        }

        return REFRESHABLE_GUI_TITLE_KEYS.contains(key)
            && SHIPPED_BLACK_GUI_TITLE_DEFAULTS.contains(diskValue.trim());
    }

    public static Optional<String> replacementForDiskValue(String key, String diskValue, String bundledValue) {
        if (key == null || diskValue == null) {
            return Optional.empty();
        }

        String exactReplacement = EXACT_REFRESH_REPLACEMENTS
            .getOrDefault(key, Map.of())
            .get(diskValue.trim());
        if (exactReplacement != null) {
            return Optional.of(exactReplacement);
        }

        if (bundledValue != null
            && BUNDLED_VALUE_REFRESHES.getOrDefault(key, Set.of()).contains(diskValue.trim())) {
            return Optional.of(bundledValue);
        }

        if (bundledValue != null
            && REFRESHABLE_GUI_TITLE_KEYS.contains(key)
            && SHIPPED_BLACK_GUI_TITLE_DEFAULTS.contains(diskValue.trim())) {
            return Optional.of(bundledValue);
        }

        return Optional.empty();
    }
}
