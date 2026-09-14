# Local data flow

Enabling the plugin and saving a pairing key authorizes selected GE observations to the user's EVI bridge. The only destination is `http://127.0.0.1:51743/api/events`. HTTP redirects and system proxies are disabled. No arbitrary destination is configurable.

The plugin has no HTTP server and accepts no external commands. The companion bridge has a browser API; its policy implications are explicitly awaiting RuneLite clarification.

The account pseudonym is SHA-256 over a random locally persisted salt and RuneLite's account profile key. Special world-type flags add an economy namespace so their trades cannot match ordinary-world trades. Ordinary world flags such as membership and PvP retain the original pseudonym. The raw profile key and character name are not transmitted. The plugin does not read inventory, bank, chat, login passwords, session credential files, player positions, or other players' information.

Local files owned by this plugin:

- `.runelite/evi-live/plugin-key.txt`: a bridge authorization token, not a Jagex login credential.
- `.runelite/evi-live/identity-salt.txt`: a random salt used to keep account labels pseudonymous.

The plugin writes neither file into RuneLite's synced settings. The sidebar masks key input and clears it after saving; keys are not intentionally logged. Existing software running as the same operating-system user can access these files. Deleting the salt changes account pseudonyms and prevents continuity with older records.

The queue exists only in memory. The companion stores records separately; disabling the plugin stops collection but does not erase previously recorded history. The bridge, browser backups, and personal scanner are separate from this plugin repository.
