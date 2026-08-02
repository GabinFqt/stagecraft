# FTB Library / FTB Teams / FTB Quests

Chapters auto-detects [FTB Library], [FTB Teams], and [FTB Quests] at runtime — no config flag, no extra command. Drop the FTB jars into the same instance as Chapters and the integration boots itself. If any of them is missing, the corresponding feature is silently skipped (Chapters falls back to per-player attachment storage).

[FTB Library]: https://www.curseforge.com/minecraft/mc-mods/ftb-library-forge
[FTB Teams]: https://www.curseforge.com/minecraft/mc-mods/ftb-teams-forge
[FTB Quests]: https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge

## What you get out of the box

When **FTB Library** is loaded, Chapters registers itself as the active stage provider via FTB Library's `StageHelper`. From that point on:

| FTB Quests feature | What it does with Chapters | Setup |
| --- | --- | --- |
| **Stage Reward** | Grants a chapter id to the claimer on quest completion | Add a *Stage* reward, type the stage id (e.g. `mypack:tier2`) |
| **Stage Task** | Marks the task complete when the player has the chapter | Add a *Stage* task, type the stage id |
| **"Stage Required"** field on a quest/chapter | Gates the quest/chapter behind a chapter id | Edit the quest/chapter, set "Stage Required" |

Stage ids are just chapter ids — no special prefix, no namespace mapping. Use the same ids you put in your `data/<namespace>/chapters/stages/*.json` or your KubeJS `ChaptersEvents.defineStage('mypack:tier2', …)` calls.

## Team-wide unlocks (with FTB Teams)

When **FTB Teams** is also loaded, chapter unlocks become **team-scoped** instead of per-player. They are stored on the player's current team via FTB Teams' built-in `TEAM_STAGES` property. Concretely:

- Every member of a `PartyTeam` shares unlocks instantly. Claiming a Stage Reward, running `/chapters add`, calling `PlayerStages.of(player).add(...)` from KubeJS — all of them write to the team store and trigger the inventory auditor + JEI hide/reveal for every online member.
- A solo player has their own personal team (`PlayerTeam`), so their unlocks stay tied to them as long as they don't join a party.
- When a player **joins** a party, Chapters **merges** their previous personal stages with the party's stages (union). Everyone in the party then has the combined set.
- When a player **leaves** a party, they keep a **snapshot** of the party's stages on their new personal team, but that copy is no longer synced with the party (further unlocks on either side stay separate).
- The very first time a player logs in after FTB Teams is added to a pre-existing world, Chapters migrates any pre-existing per-player attachment unlocks into their personal `TEAM_STAGES`. Party teams are skipped during migration to avoid leaking personal stages into a party.

Everything routes through the same path — `/chapters add/remove`, KubeJS, datapack hooks, FTB Quests Stage Reward, the inventory auditor, JEI sync, and the FTB Teams GUI all converge on `TEAM_STAGES`.

If FTB Teams is **absent**, the per-player attachment store is used unchanged — single-player worlds and servers without FTB Teams behave exactly like Chapters 1.0.

## Conditional rewards via Custom Reward + KubeJS

FTB Quests' **Custom Reward** is the escape hatch when you want logic on top of a chapter grant — granting one of several stages, conditional on the player's current state, etc. Pair it with KubeJS:

```js
// kubejs/server_scripts/ftbquests_chapter_reward.js
FTBQuestsEvents.customReward('mypack:grant_tier2', event => {
  const player = event.player
  const stage = 'mypack:tier2'

  // Only grant if the player doesn't already have the stage
  if (PlayerStages.of(player).has(stage)) return

  PlayerStages.of(player).add(stage)
  player.tell(Text.green(`Unlocked ${stage}!`))
})
```

In the quest editor, add a **Custom** reward and set the id to `mypack:grant_tier2`. The reward will route through Chapters' team-aware writer, so a party member claiming it shares the unlock with the rest of the party.

A working version of the script lives at [`examples/kubejs/server_scripts/ftbquests_chapter_reward.js`](https://github.com/GabinFqt/chapters/blob/main/examples/kubejs/server_scripts/ftbquests_chapter_reward.js).

## Notes for pack authors

- The integration is opt-in by **mod presence**, not by config. Authors who don't ship FTB get the original per-player behaviour.
- FTB Library, FTB Teams, FTB Quests are all declared as `optional` dependencies in the mod metadata. None of them are required to load Chapters.
- If you only need the SPI bridge (Stage Reward / Stage Task / "Stage Required"), installing **FTB Library + FTB Quests** is enough — FTB Teams is only required for team-wide unlock sharing.
- Chapters' own `/chapters reload` only refreshes stage **definitions**; it does not touch FTB Teams' stored team stages. To wipe team stages use FTB Teams' own commands.
