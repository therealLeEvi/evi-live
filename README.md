# EVI Live (Local)

Development candidate for RuneLite Plugin Hub review. **Not submitted or approved.**

EVI Live passively observes your own Grand Exchange offers and sends selected observations to an EVI bridge on your computer. It never places offers, clicks menus, inserts chat, or automates trading.

## Current review question

RuneLite's [rejected features policy](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features) lists plugins exposing player information over HTTP. This plugin is an outbound-only HTTP client with a fixed loopback destination; it does not open a listening socket in RuneLite. The companion application exposes authenticated records to its local browser UI. The acceptability of that complete design needs maintainer clarification before submission. Localhost alone is not assumed to exempt it.

## Setup

1. Start your EVI local bridge on `127.0.0.1:51743`.
2. Enable EVI Live (Local) and open its EVI sidebar button.
3. Paste the **RuneLite plugin key** from the bridge window into the masked pairing field and save.
4. Log in and watch the connection message. Missing/invalid pairing, refused authentication, and connection failures are displayed in the sidebar.

This repository contains the plugin only. The existing personal EVI package provides the bridge/scanner; it is not included here because it contains personal embedded trade history. A clean, publicly reviewable companion distribution is required before a public release.

The key is stored in `.runelite/evi-live/plugin-key.txt`, outside synced RuneLite configuration. Existing correctly paired installations are compatible. An existing key file may be updated manually too; restart the plugin to reload it. No Jagex credentials are requested or read by this plugin.

## Observations and limitations

Packets include eight offer slots, item ID/name, state, offer price, total and filled quantities, the RuneLite cumulative spent counter, capture timestamp, sequence number, random session/offer IDs, and a salted account pseudonym. Logged-out snapshots contain no offers. The counter is not asserted to equal after-tax proceeds.

Offers present on login are incomplete baselines. Only zero-filled offers following an observed empty slot qualify as observed from their start. Retries keep identical sequence numbers, and the companion deduplicates observations. The bounded memory queue holds up to 512 packets; overflow resets observation conservatively. Unsent packets can be lost when RuneLite closes. No complete flip is fabricated from missing coverage.

## Build and tests

Use Java 11 and the included Gradle wrapper:

```powershell
.\gradlew.bat clean build
.\gradlew.bat run
```

`check` runs synthetic observation, pairing-key, sidebar, economy-separation and delivery tests. The development launcher uses RuneLite's standard external plugin registration. Tests do not place trades or launch a game. Java 11 direct compilation is verified; full Gradle/Hub checks and live candidate UI validation are pending. The local sandbox's Gradle build resolves dependencies but fails while closing Java compiler resources with a Windows access error; this must still pass in a normal build environment before submission.

## Privacy and review surface

See [PRIVACY.md](PRIVACY.md). Production code is plain Java. There is no reflection, native code, downloaded executable code, child-process execution, remote configuration, or inbound game-control channel in the plugin. Synthetic tests currently use reflection/proxies to simulate RuneLite interfaces; these are test sources only and are not included in the production JAR.

Maintainer: **therealLeEvi**. Publishing location and final submission commit are not yet established.
