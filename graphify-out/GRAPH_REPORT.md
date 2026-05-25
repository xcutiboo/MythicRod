# Graph Report - MythicRod  (2026-05-21)

## Corpus Check
- 106 files · ~77,515 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1571 nodes · 4968 edges · 22 communities detected
- Extraction: 52% EXTRACTED · 48% INFERRED · 0% AMBIGUOUS · INFERRED: 2362 edges (avg confidence: 0.8)
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
- [[_COMMUNITY_Community 21|Community 21]]

## God Nodes (most connected - your core abstractions)
1. `BrigadierCommandManager` - 81 edges
2. `DropManager` - 73 edges
3. `EditDropMenu` - 73 edges
4. `ConfigManager` - 54 edges
5. `FishingListener` - 50 edges
6. `BaseMenu` - 45 edges
7. `MythicRod` - 39 edges
8. `DropsMenu` - 36 edges
9. `ConfigMenu` - 35 edges
10. `GUIManager` - 33 edges

## Surprising Connections (you probably didn't know these)
- `DropManager` --implements--> `DropCatalog`  [EXTRACTED]
  mythicrod-common/src/main/java/io/xcutiboo/mythicrod/drops/DropManager.java →   _Bridges community 2 → community 10_
- `FakeRuntime` --implements--> `MythicRodRuntime`  [EXTRACTED]
  mythicrod-common/src/test/java/io/xcutiboo/mythicrod/config/PlayerPreferencesTest.java →   _Bridges community 8 → community 1_
- `MythicRod` --implements--> `MythicRodRuntime`  [EXTRACTED]
  mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/paper/MythicRod.java →   _Bridges community 1 → community 9_
- `FishingListener` --implements--> `Listener`  [EXTRACTED]
  mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/paper/fishing/FishingListener.java →   _Bridges community 7 → community 4_
- `FoliaSchedulerService` --implements--> `PlatformScheduler`  [EXTRACTED]
  mythicrod-paper/src/main/java/io/xcutiboo/mythicrod/paper/scheduler/FoliaSchedulerService.java →   _Bridges community 15 → community 1_

## Communities

### Community 0 - "Community 0"
Cohesion: 0.04
Nodes (8): BaseMenu, ConfigMenu, DropsMenu, EditDropMenu, MainHubMenu, RodMenu, StatsMenu, PlatformCommandSender

### Community 1 - "Community 1"
Cohesion: 0.01
Nodes (28): FakePlatformServer, FakeRuntime, MapPlatformConfiguration, EmptyConfig, FakeRuntime, FakeServer, PaperConfiguration, FakePlayer (+20 more)

### Community 2 - "Community 2"
Cohesion: 0.05
Nodes (7): DropCatalog, DropManager, DropManagerTest, ItemBuilder, PlatformConfiguration, PlatformDrop, PlatformItem

### Community 3 - "Community 3"
Cohesion: 0.07
Nodes (3): BrigadierCommandManager, ValidationCounts, getConfigValue()

### Community 4 - "Community 4"
Cohesion: 0.04
Nodes (10): PlayerDataService, StatisticsPlayerListener, GUIManager, MenuFactory, getMenu(), Listener, BaseMenu, RunnableClickHandler (+2 more)

### Community 5 - "Community 5"
Cohesion: 0.05
Nodes (10): empty(), PlayerStatSnapshotTest, LanguageManager, LanguageManagerTest, ItemBuilderFormattingTest, LanguageSwitchMenu, ConfiguredText, ConfiguredTextTest (+2 more)

### Community 6 - "Community 6"
Cohesion: 0.06
Nodes (4): ConfigManager, ConfigManagerTest, LanguageFileLoaderTest, MetricsReporter

### Community 7 - "Community 7"
Cohesion: 0.06
Nodes (6): CustomDropTest, FishingListener, RodFactory, PlatformInventory, PlatformItemFactory, PlatformWorld

### Community 8 - "Community 8"
Cohesion: 0.04
Nodes (7): BlockingLoad, BlockingSave, FakePlatformConfiguration, FakePlatformServer, FakeRuntime, PlayerPreferencesTest, MapPlatformConfiguration

### Community 9 - "Community 9"
Cohesion: 0.05
Nodes (8): LanguageFileLoader, PlayerPreferences, NexoItemProvider, JavaPlugin, MythicRod, PlatformServer, MythicRodSpigot, PrettyLogger

### Community 10 - "Community 10"
Cohesion: 0.04
Nodes (11): ExternalDropProvider, MythicRodAPI, Result, ResultTest, DropCatalog, DropLoadReport, ItemFactory, PaperPlatformItem (+3 more)

### Community 11 - "Community 11"
Cohesion: 0.06
Nodes (8): MythicRodServices, LanguageOverridePolicy, LanguageOverridePolicyTest, FishingListenerCatchMessageTest, MiniMessageMigrator, MiniMessageMigratorTest, StringFormatting, StringFormattingTest

### Community 12 - "Community 12"
Cohesion: 0.09
Nodes (4): StatisticsManager, StatisticsManagerTest, PlayerStats, PlayerStatsTest

### Community 13 - "Community 13"
Cohesion: 0.04
Nodes (7): Cancellable, CustomDrop, Event, MythicRodFishCatchEvent, MythicRodRewardRollEvent, MythicRodStatsUpdateEvent, PlatformDrop

### Community 14 - "Community 14"
Cohesion: 0.07
Nodes (6): hit(), isExternal(), miss(), PaperMythicRodAPI, reroll(), DropSelector

### Community 15 - "Community 15"
Cohesion: 0.12
Nodes (9): PaperLocation, getPitch(), getWorldName(), getX(), getY(), getYaw(), getZ(), PlatformTask (+1 more)

### Community 16 - "Community 16"
Cohesion: 0.16
Nodes (2): ParticleOptions, ParticleOptionsTest

### Community 17 - "Community 17"
Cohesion: 0.21
Nodes (5): fromConfigValue(), next(), previous(), RewardDeliveryMode(), RewardDeliveryModeTest

### Community 18 - "Community 18"
Cohesion: 0.43
Nodes (1): DropConfigurationRecordTest

### Community 19 - "Community 19"
Cohesion: 0.67
Nodes (1): MythicRodKeys

### Community 20 - "Community 20"
Cohesion: 0.67
Nodes (1): PermissionNodes

### Community 21 - "Community 21"
Cohesion: 0.67
Nodes (1): MenuItemFactory

## Knowledge Gaps
- **2 isolated node(s):** `DropLoadReport`, `ValidationCounts`
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Community 16`** (19 nodes): `.suggestParticles()`, `ParticleOptions.java`, `ParticleOptionsTest.java`, `.validateParticleSetting()`, `ParticleOptions`, `.configurableNames()`, `.isConfigurableParticleName()`, `.move()`, `.nextSuggested()`, `.normalize()`, `.ParticleOptions()`, `.suggestedNames()`, `.supportsDefaultData()`, `ParticleOptionsTest`, `.cyclesUnknownValuesBackToFirstSuggestion()`, `.everyCurrentPaperParticleHasSafeConfiguredDataHandling()`, `.exposesEveryPaperParticleNameForCommandSuggestions()`, `.normalizesCommandInputBeforeValidation()`, `.suggestedParticlesAreValidPaperParticleNames()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 18`** (7 nodes): `DropConfigurationRecordTest`, `.build()`, `.collectionsAreDefensivelyCopied()`, `.identifierAndPrimitiveFieldsReflectInput()`, `.rejectsNonPositiveWeightAndAmount()`, `.rejectsNullOrBlankIdentifier()`, `DropConfigurationRecordTest.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 19`** (3 nodes): `MythicRodKeys`, `.MythicRodKeys()`, `MythicRodKeys.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 20`** (3 nodes): `PermissionNodes`, `.PermissionNodes()`, `PermissionNodes.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Community 21`** (3 nodes): `MenuItemFactory`, `.MenuItemFactory()`, `MenuItemFactory.java`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `DropManager` connect `Community 2` to `Community 0`, `Community 3`, `Community 7`, `Community 10`, `Community 12`?**
  _High betweenness centrality (0.071) - this node is a cross-community bridge._
- **Why does `MythicRod` connect `Community 9` to `Community 0`, `Community 1`, `Community 3`, `Community 4`, `Community 6`, `Community 16`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Why does `FakePlatformServer` connect `Community 8` to `Community 1`?**
  _High betweenness centrality (0.038) - this node is a cross-community bridge._
- **What connects `DropLoadReport`, `ValidationCounts` to the rest of the system?**
  _2 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.04 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.01 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._