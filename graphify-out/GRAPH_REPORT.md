# Graph Report - MythicRod  (2026-05-20)

## Corpus Check
- 96 files · ~70,785 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1335 nodes · 4037 edges · 21 communities detected
- Extraction: 54% EXTRACTED · 46% INFERRED · 0% AMBIGUOUS · INFERRED: 1865 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]

## God Nodes (most connected - your core abstractions)
1. `EditDropMenu` - 73 edges
2. `DropManager` - 67 edges
3. `BrigadierCommandManager` - 64 edges
4. `ConfigManager` - 54 edges
5. `BaseMenu` - 45 edges
6. `FishingListener` - 44 edges
7. `MythicRod` - 33 edges
8. `GUIManager` - 27 edges
9. `DropsMenu` - 27 edges
10. `PlayerStats` - 25 edges

## Surprising Connections (you probably didn't know these)
- `DropManager` --implements--> `DropCatalog`  [EXTRACTED]
  mythicrod-common/src/main/java/io/xcutiboo/mythicrod/drops/DropManager.java →   _Bridges community 1 → community 6_
- `FakeRuntime` --implements--> `MythicRodRuntime`  [EXTRACTED]
  mythicrod-common/src/test/java/io/xcutiboo/mythicrod/config/PlayerPreferencesTest.java →   _Bridges community 3 → community 7_
- `FakeRuntime` --implements--> `MythicRodRuntime`  [EXTRACTED]
  mythicrod-common/src/test/java/io/xcutiboo/mythicrod/metrics/StatisticsManagerTest.java →   _Bridges community 3 → community 5_
- `FishingListener` --implements--> `Listener`  [EXTRACTED]
  mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/paper/fishing/FishingListener.java →   _Bridges community 2 → community 4_
- `FoliaSchedulerService` --implements--> `PlatformScheduler`  [EXTRACTED]
  mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/paper/scheduler/FoliaSchedulerService.java →   _Bridges community 13 → community 3_

## Communities

### Community 0 - "Community 0"
Cohesion: 0.05
Nodes (6): ConfigMenu, DropsMenu, EditDropMenu, StatsMenu, PlatformCommandSender, PlatformItemFactory

### Community 1 - "Community 1"
Cohesion: 0.04
Nodes (6): DropManager, DropManagerTest, DropSelector, PlatformConfiguration, PlatformDrop, PlatformItem

### Community 2 - "Community 2"
Cohesion: 0.06
Nodes (4): BrigadierCommandManager, getConfigValue(), FishingListener, PlatformWorld

### Community 3 - "Community 3"
Cohesion: 0.02
Nodes (19): FakePlatformServer, FakeRuntime, MapPlatformConfiguration, PaperConfiguration, StatsCacheRemovalListener, MapPlatformConfiguration, StatisticsManagerTest, MythicRodRuntime (+11 more)

### Community 4 - "Community 4"
Cohesion: 0.04
Nodes (9): PlayerDataService, StatisticsPlayerListener, GUIManager, Listener, BaseMenu, RunnableClickHandler, PlatformInventory, PlatformPlayer (+1 more)

### Community 5 - "Community 5"
Cohesion: 0.04
Nodes (11): Result, LanguageFileLoader, PlayerPreferences, ItemFactory, NexoItemProvider, JavaPlugin, FakeRuntime, MythicRod (+3 more)

### Community 6 - "Community 6"
Cohesion: 0.03
Nodes (14): ExternalDropProvider, MythicRodAPI, isExternal(), DropCatalog, DropLoadReport, FakePlayer, PaperPlatformItem, MythicRodAPI (+6 more)

### Community 7 - "Community 7"
Cohesion: 0.05
Nodes (7): BlockingLoad, BlockingSave, FakePlatformConfiguration, FakePlatformServer, FakeRuntime, PlayerPreferencesTest, MapPlatformConfiguration

### Community 8 - "Community 8"
Cohesion: 0.06
Nodes (7): ConfigManager, ConfigManagerTest, LanguageFileLoaderTest, fromConfigValue(), next(), previous(), RewardDeliveryMode()

### Community 9 - "Community 9"
Cohesion: 0.04
Nodes (9): Cancellable, DropCatalog, CustomDrop, Event, MythicRodFishCatchEvent, MythicRodRewardRollEvent, ItemBuilder, ItemBuilderFormattingTest (+1 more)

### Community 10 - "Community 10"
Cohesion: 0.06
Nodes (4): PaperMythicRodAPI, StatisticsManager, PlayerStats, PlayerStatsTest

### Community 11 - "Community 11"
Cohesion: 0.06
Nodes (8): MythicRodServices, LanguageOverridePolicy, LanguageOverridePolicyTest, FishingListenerCatchMessageTest, MiniMessageMigrator, MiniMessageMigratorTest, StringFormatting, StringFormattingTest

### Community 12 - "Community 12"
Cohesion: 0.08
Nodes (5): empty(), LanguageManager, LanguageSwitchMenu, ConfiguredText, MessageFormatter

### Community 13 - "Community 13"
Cohesion: 0.12
Nodes (4): PaperLocation, PlatformLocation, PlatformTask, FoliaSchedulerService

### Community 14 - "Community 14"
Cohesion: 0.08
Nodes (7): BaseMenu, MenuFactory, MythicRodMenuHolder, InventoryHolder, RodFactory, MainHubMenu, RodMenu

### Community 15 - "Community 15"
Cohesion: 0.16
Nodes (2): ParticleOptions, ParticleOptionsTest

### Community 16 - "Community 16"
Cohesion: 0.29
Nodes (3): DropGenerationException, MythicRodConfigurationException, RuntimeException

### Community 17 - "Community 17"
Cohesion: 0.67
Nodes (1): ConfigKeys

### Community 18 - "Community 18"
Cohesion: 0.67
Nodes (1): MythicRodKeys

### Community 19 - "Community 19"
Cohesion: 0.67
Nodes (1): PermissionNodes

### Community 20 - "Community 20"
Cohesion: 0.67
Nodes (1): MenuItemFactory

## Knowledge Gaps
- **1 isolated node(s):** `DropLoadReport`
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 15`** (19 nodes): `.suggestParticles()`, `.validateParticleSetting()`, `ParticleOptions.java`, `ParticleOptionsTest.java`, `ParticleOptions`, `.configurableNames()`, `.isConfigurableParticleName()`, `.move()`, `.nextSuggested()`, `.normalize()`, `.ParticleOptions()`, `.suggestedNames()`, `.supportsDefaultData()`, `ParticleOptionsTest`, `.cyclesUnknownValuesBackToFirstSuggestion()`, `.everyCurrentPaperParticleHasSafeConfiguredDataHandling()`, `.exposesEveryPaperParticleNameForCommandSuggestions()`, `.normalizesCommandInputBeforeValidation()`, `.suggestedParticlesAreValidPaperParticleNames()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 17`** (3 nodes): `ConfigKeys`, `.ConfigKeys()`, `ConfigKeys.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 18`** (3 nodes): `MythicRodKeys`, `.MythicRodKeys()`, `MythicRodKeys.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 19`** (3 nodes): `PermissionNodes`, `.PermissionNodes()`, `PermissionNodes.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 20`** (3 nodes): `MenuItemFactory`, `.MenuItemFactory()`, `MenuItemFactory.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DropManager` connect `Community 1` to `Community 0`, `Community 2`, `Community 10`, `Community 6`?**
  _High betweenness centrality (0.070) - this node is a cross-community bridge._
- **Why does `MythicRod` connect `Community 5` to `Community 0`, `Community 1`, `Community 2`, `Community 3`, `Community 4`, `Community 10`, `Community 14`, `Community 15`?**
  _High betweenness centrality (0.068) - this node is a cross-community bridge._
- **What connects `DropLoadReport` to the rest of the system?**
  _1 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.04 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.06 - nodes in this community are weakly interconnected._
- **Should `Community 3` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._