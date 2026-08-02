**Chapters** is a progression mod for **NeoForge 26.1.2** (requires **Java 25**). You gate **items, fluids, Mekanism chemicals**, and **recipes by id** behind named *stages* using **datapacks** and optionally **KubeJS**, with optional **JEI** integration so players see what they can actually use.

- **[Download releases](https://github.com/GabinFqt/chapters/releases)** (jar for your mods folder)
- **Something wrong or missing from the wiki?** [Open an issue](https://github.com/GabinFqt/chapters/issues)

## Commands (quick reference)

All require permission level as usual for multiplayer.

| Command | What it does |
| --- | --- |
| `/chapters add <player> <stage>` | Grants a stage (e.g. `mypack:tier2`) |
| `/chapters remove <player> <stage>` | Revokes it |
| `/chapters list <player>` | Lists that player’s current stages |
| `/chapters check <player> <stage>` | `true` / `false` |
| `/chapters reload` | Reloads stage **definitions** (after datapack or script edits that affect gates) |

## Getting started as a pack author

1. Define stages in **datapack JSON** under `data/<namespace>/chapters/stages/` **and/or** with **KubeJS** — see [[Stages-datapack]] and [[KubeJS]].
2. Run **`/reload`** after changing datapacks (operator).
3. KubeJS: stage definitions typically apply on **`ServerEvents.loaded`**; reloading behaviour follows your usual KubeJS/server restart habits.
4. Grant progression with **`/chapters add`** or from your own **`PlayerStages.of(player).add(...)`** logic tied to quests, advancements, etc. — [[Examples]] for patterns.

## Wiki pages

| Page | Purpose |
| --- | --- |
| [[Stages-datapack]] | Where to put JSON, keys, `#` tags, `@mods` |
| [[KubeJS]] | `defineStage`, `fluid:`, `recipe:`, `PlayerStages` |
| [[Examples]] | Copy-paste setups (tiers, fluids, recipes, Mekanism) |
| [[FTB-integration]] | FTB Library / FTB Teams / FTB Quests (Stage Reward, team-wide unlocks) |
| [[JEI-and-limitations]] | What JEI hides and what crafting is blocked |
| [[Troubleshooting]] | datapack not applying, conflicting stages, JEI quirks |

### Sample packs

You can copy a **tutorial datapack** and **KubeJS snippets** straight from **[this folder on GitHub](https://github.com/GabinFqt/chapters/tree/main/examples)** (`examples/datapack/tutorial`, `examples/kubejs`). No need to build the mod yourself — grab the jar from Releases and paste the samples into your world or modpack workspace.
