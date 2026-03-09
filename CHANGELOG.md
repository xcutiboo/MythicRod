# Changelog

## [1.0.2] - 2025-12-16

### Added

- Multi-module project structure (common, paper, spigot)
- Paper: Native Brigadier command system
- Spigot: Full feature parity with Paper

### Changed

- Split into platform-specific implementations
- Paper uses native Adventure API
- Spigot bundles Adventure Platform (relocated to prevent conflicts)

### Fixed

- Hook state edge case where waiting after bite could bypass custom drops

## [1.0.1] - 2025-12-14

### Fixed

- Hook state cleanup preventing custom drops after delayed catch

## [1.0.0] - 2025-12-13

### Added

- Custom fishing drop system
- Biome-specific drops
- Permission-based categories
- Statistics tracking and leaderboards
- Item customization (names, lore, enchantments, glow)
- GUI menus
- Multi-language support (English, Japanese)
- Sound and particle effects
- Developer API
