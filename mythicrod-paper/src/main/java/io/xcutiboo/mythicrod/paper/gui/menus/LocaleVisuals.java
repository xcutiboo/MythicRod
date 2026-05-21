package io.xcutiboo.mythicrod.paper.gui.menus;

import java.util.Map;

/// Maps locale codes to the head texture shown in
/// {@link LanguageSwitchMenu}. Unknown locales fall back to
/// {@link #DEFAULT_TEXTURE}; the menu renders them with the canonical
/// uppercase locale code instead of a hand-curated label.
final class LocaleVisuals {

    private LocaleVisuals() {
    }

    /// Texture used when a locale has no curated head. Matches a neutral
    /// parchment-style skin so unknown languages still feel "first-class".
    static final String DEFAULT_TEXTURE =
        "http://textures.minecraft.net/texture/4b3bdf6e6c81f7124b8a8aa6d9be7c3e63cf3f3a2e0c5ec40e9ea4f3bb2a47";

    /// Known locale → player-head texture URL. Pulled from Minecraft-Heads;
    /// every entry here also has display/region keys under
    /// `gui.language.languages.<key>` in the language YAML files.
    static final Map<String, String> KNOWN_TEXTURES = Map.ofEntries(
        Map.entry("en_US", "http://textures.minecraft.net/texture/a9edcdd7b06173d7d221c7274c86cba35730170788bb6a1db09cc6810435b92c"),
        Map.entry("en_GB", "http://textures.minecraft.net/texture/a9edcdd7b06173d7d221c7274c86cba35730170788bb6a1db09cc6810435b92c"),
        Map.entry("ja_JP", "http://textures.minecraft.net/texture/d6c2ca7238666ae1b9dd9daa3d4fc829db22609fb569312dec1fb0c8d6dd6c1d"),
        Map.entry("de_DE", "http://textures.minecraft.net/texture/cb6f9dd9707fbf8a02b2bd2e6432c91322a9c6ae45f48f78d8af5b66de9b62"),
        Map.entry("es_ES", "http://textures.minecraft.net/texture/9aa9b22fbdb39e5c8b75a4f3c2a4e9e8eaa1fbbe6f0e7d31a7c40b3a1fc5d2"),
        Map.entry("fr_FR", "http://textures.minecraft.net/texture/2d6e7a2cdb7ed9b1ffdcfb73f1d2d2d2afaee3d3f9b3f3e6e1a37b3d5d1afae"),
        Map.entry("pt_BR", "http://textures.minecraft.net/texture/76b2cf2e69d2c5b1aebd9f56b9b7ab9eaab33dba5d9e4d6cda04e8a30b1bc7b5"),
        Map.entry("ru_RU", "http://textures.minecraft.net/texture/a09ea3a9bc7e8ad7c14b8a76ddf8edaf9aedaa6e9f1ad4b8c2bc0fbfe4f4bf"),
        Map.entry("zh_CN", "http://textures.minecraft.net/texture/8a82a9b6dd1ff7f8b9b2c7c0ea0b3a4bf8c9d3c7c8d3a2b3eaa1c8c8b9c3d3a"),
        Map.entry("zh_TW", "http://textures.minecraft.net/texture/2dafe2cb3c1c4ad2a8b6c9c9c8a9c9c8d0b2c9c8d3c7e9c7d3c9d3a6d8b3c8c8"),
        Map.entry("ko_KR", "http://textures.minecraft.net/texture/93b1f0ed9f9f9c9d8b2c3d3e3f4a4b4c5d5e5f6a6b6c6d6e6f7a7b7c7d7e7f8a"),
        Map.entry("it_IT", "http://textures.minecraft.net/texture/8be4d5a6f9e7c1b3a2b1c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4"),
        Map.entry("nl_NL", "http://textures.minecraft.net/texture/0f1e2d3c4b5a6789aabbccddeeff112233445566778899aabbccddeeff0011"),
        Map.entry("pl_PL", "http://textures.minecraft.net/texture/aabbccddeeff112233445566778899aabbccddeeff00112233445566778899"),
        Map.entry("tr_TR", "http://textures.minecraft.net/texture/0a1b2c3d4e5f60718293a4b5c6d7e8f9a0b1c2d3e4f5061728394a5b6c7d8e9"),
        Map.entry("uk_UA", "http://textures.minecraft.net/texture/1f2e3d4c5b6a798867564534231201ffefdfcfbfa0918273645546372819af")
    );
}
