# Troubleshooting

## Locks never apply

- **Chapters** jar is in **`mods`** (client **and** server if multiplayer).
- Your datapack is **enabled for the world** and **`/reload`** completed without errors (*operator* permission).
- JSON sits under **`data/<namespace>/chapters/stages/*.json`** (folder name **`stages`**, plural).

## Worked once, stopped after edits

**KubeJS** usually needs whatever restart flow you normally use **`ServerEvents.loaded`** doesn’t rerun on every datapack **`/reload`**.

Run **`/chapters reload`** after big datapack swaps, then **`/chapters check`** your test player again.

## “They have the stage” but loot still disappears

Sounds like **multiple stages for one item** rule: overlapping JSON files each reference the same asset. Use **`/chapters list`** and reconcile every datapack/`defineStage` that touches that asset — explained under **“When the same item appears in more than one file”** in [[Stages-datapack]].

## Mekanism chemicals unaffected

Mod missing → harmless JSON, zero chemicals indexed.

GUI transfers vs unattended automation can differ — don’t rely on pipes alone mirroring survival clicking.

## Odd JEI

Reconnect; keep **matching Chapters jars** client/server; flip **`/chapters add/remove`** once to poke a fresh sync after big script edits.

## Client crash with KubeJS (Better Advanced Tooltips)

KubeJS `26.1.2-8.0.4` embeds **Better Advanced Tooltips** build.8, which breaks on Minecraft 26.1.2. Chapters **2.0** Jar-in-Jars BAT **build.9+** so NeoForge selects the fixed copy. If you still crash, update Chapters, or install a standalone BAT build.9+ / a newer KubeJS that embeds it.
