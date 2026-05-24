# MythicRod

MythicRod は、最新の Paper サーバー向けに作られたカスタム釣りプログレ
ッションプラグインです。バイオームフィルター、権限ゲート、ゲーム内の
ドロップエディター、統計、そして小さな公開 API を備えた、重み付き
ルートテーブルを提供します。

## どこから始めるか

| 役割 | 最初に読むページ |
| --- | --- |
| サーバーに MythicRod を導入する | [インストール](installation.md) のあと [コマンド](commands.md) |
| ドロップやロッド、メッセージを調整する | [設定](configuration.md) と [ルートテーブル](loot-tables.md) |
| ライブサーバーをデバッグする | [トラブルシューティング](troubleshooting.md)、`/mythicrod status`、`/mythicrod validate` |
| MythicRod を翻訳する | [ローカライズ](localization.md)、[Crowdin](localization/crowdin.md) |
| ほかのプラグインから API を呼び出す | [開発者 API](developer-api.md) |
| 公開リリースを切る | [リリースガイド](release.md) と [チェックリスト](release/checklist.md) |

## ビルド済みのもの

| 項目 | 値 |
| --- | --- |
| プラグイン | `2026.1.0` (CalVer、`年.リリース.パッチ`) |
| API | Paper `26.1.2` (Minecraft `26.1.2`) |
| Java | 25 以上 |
| 任意の統合 | Nexo (`nexo:*` 識別子) |
| 同梱言語 | `en_US`、`ja_JP` (他は Crowdin から同期) |
| スケジューラ | Paper 優先、Folia の所有者スレッドの受け渡しを検証済み |

## ステータス

- SonarCloud: バグ 0 / 脆弱性 0 / コードスメル 0 / ホットスポット 0
- bStats: プラグイン ID `31484`、15 のカスタムチャート ([ダッシュボード](https://bstats.org/plugin/bukkit/MythicRod/31484))
- Crowdin: [crowdin.com/project/mythicrod](https://crowdin.com/project/mythicrod) (en_US がソース、ja_JP は同梱)
- リリース: [github.com/xcutiboo/MythicRod/releases](https://github.com/xcutiboo/MythicRod/releases)
