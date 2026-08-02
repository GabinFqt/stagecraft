# Chapters 2.0

Major port to **Minecraft / NeoForge 26.1.2** (Java **25**).

## Changes
- Full API port for NeoForge 26.1 (`Identifier`, networking, attachments, FTB/JEI/KubeJS adaptations).
- Vanilla bucket pickup/place and cauldron interactions are gated when fluids are locked (replaces the removed fill-bucket event path).
- Mekanism chemical compat is compiled out until a 26.1 Mekanism release exists (`enable_mekanism=false`).
- Works around KubeJS embedding a broken Better Advanced Tooltips build.8 by Jar-in-Jaring BAT **2601.1.0-build.9+** so NeoForge selects the fixed jar at runtime.
- Dev runs: Alice (`runClient`) and Bob (`runClientBob`) plus shared root `kubejs/` linking for dual-client testing.

## Requirements
- Minecraft 26.1.2
- NeoForge 26.1.x
- Java 25
- JEI (optional, to hide locked recipes/items client-side)
- KubeJS (optional)
- FTB Library / FTB Teams / FTB Quests (all optional; auto-detected at runtime)
- Mekanism (optional; not available for 26.1 in this release)

---

# Chapters 1.1

FTB Library / FTB Teams / FTB Quests integration.

## Changes
- Chapters now registers itself as FTB Library's active stage provider when `ftblibrary` is installed; FTB Quests' built-in **Stage Reward**, **Stage Task**, and "Stage Required" field all use chapter ids out of the box — no extra setup required.
- When `ftbteams` is also installed, chapter unlocks are **team-scoped** through FTB Teams' `TEAM_STAGES` property: every party member instantly shares unlocks, the inventory auditor and JEI hide/reveal run for all online members, and `/chapters add/remove`, KubeJS `PlayerStages.of(player)`, and FTB Quests Stage Reward all converge on the same team-wide store.
- One-time migration on first login: pre-existing per-player attachment unlocks on a player's personal team are copied into `TEAM_STAGES` (party teams are skipped to avoid leaking personal unlocks).
- New `examples/kubejs/server_scripts/ftbquests_chapter_reward.js` sample showing the FTB Quests **Custom Reward** path for conditional chapter grants.
- New optional dependencies declared in `neoforge.mods.toml` and advertised on Modrinth/CurseForge: `ftb-library`, `ftb-teams`, `ftb-quests`.
- Workaround for an upstream FTB Teams `TEAM_STAGES` aliasing bug: `TeamProperties.TEAM_STAGES` ships with a single shared `HashSet` as its default value, and `TeamStagesHelper.updateStages` mutates that instance in place — meaning a stage write on team A leaks into every other team's first read of the property. Chapters now bypasses `TeamStagesHelper`, copies the current value into a fresh `HashSet` before mutating, and treats the shared default as empty on read, so each FTB team keeps its own isolated stage set.

## Requirements
- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- Mekanism (optional, for chemical gating)
- JEI (optional, to hide locked recipes/items client-side)
- FTB Library / FTB Teams / FTB Quests (all optional; auto-detected at runtime)

---

# Chapters 1.0

First release under the new name **Chapters** (formerly Stagecraft).

## Changes
- Full mod rename: `stagecraft` → `chapters` (mod id, Java package, asset namespace, mixins, KubeJS bindings)
- Bumped `mod_version` to `1.0`
- KubeJS API: `ChaptersEvents` (formerly `StagecraftEvents`)
- New `chapters_logo.png`

## Requirements
- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21
- Mekanism (optional, for chemical gating)
- JEI (optional, to hide locked recipes/items client-side)
