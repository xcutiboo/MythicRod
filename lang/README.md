# Language Files

This folder is documentation-only.

The runtime language source lives in `mythicrod-paper/src/main/resources/lang/`.
When the plugin starts, those files are copied into the server's deployed
`plugins/MythicRod/lang/` directory.

## Working on Translations

1. Edit `mythicrod-paper/src/main/resources/lang/en_US.yml` as the source-of-truth English file.
2. Mirror the same key structure in every other locale file under `mythicrod-paper/src/main/resources/lang/`.
3. Translate values only; keep keys and placeholders unchanged.
4. Reload the plugin or copy the built files into `plugins/MythicRod/lang/` to test them in-game.

MiniMessage is used throughout the live lang files.
