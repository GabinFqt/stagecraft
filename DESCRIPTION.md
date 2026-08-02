# Chapters

This file holds **two paste-ready descriptions**: [Modrinth](#modrinth) (markdown with tables, code, and inline code) and [CurseForge](#curseforge) (markdown with **#** headings, **bold**, *italic*, and lists; still no tables, fenced code, or backticks, which CurseForge handles poorly).

---

## Modrinth

**Full documentation (datapacks, KubeJS, FTB, examples): [GitHub Wiki](https://github.com/GabinFqt/chapters/wiki)**

**Chapters** is a progression mod for **NeoForge 26.1.2** inspired by *GameStages* and *ItemStages*. It lets you split your modpack into named "chapters" (stages) and gate **items, fluids, Mekanism chemicals, and recipes** behind them, so players cannot interact with locked content until you unlock the stage.

It targets pack authors who want **one integrated tool** for progression on modern NeoForge, with first-class support for **datapacks**, **KubeJS**, **JEI**, **Mekanism**, and **FTB** (Library, Teams, Quests).

### FTB Library, FTB Teams, and FTB Quests

When **FTB Library** is installed, Chapters registers as the FTB **stage provider** (`StageHelper`). **FTB Quests** can then use the built-in **Stage Reward** (grant a chapter on claim), **Stage Task** (require a chapter), and **Stage Required** on quests or chapters, all using your chapter ids with no extra wiring.

With **FTB Teams** as well, unlocks are **team-scoped** via FTB's `TEAM_STAGES`: the whole party shares chapter progress, inventory checks and JEI updates apply for every online member, and `/chapters`, KubeJS `PlayerStages`, and FTB Quests stage rewards all read and write the same team storage. Joining a team adopts that team's stages; leaving personal progress behind follows FTB's team model (see the wiki for migration details).

For branching logic (e.g. only grant if some other condition holds), combine **FTB Quests Custom Reward** with KubeJS. Example: `examples/kubejs/server_scripts/ftbquests_chapter_reward.js` in the GitHub repo.

### What you can lock

- **Items:** by id, by tag (`#minecraft:swords`), or whole mod (`@create`).
- **Fluids:** by id, tag, or mod. Buckets, placement, and NeoForge fluid transfers are checked where a server player applies.
- **Mekanism chemicals:** gas / infusion / slurry / pigment by id, tag, or mod, on Mekanism `ChemicalUtils` paths when a server player is in scope.
- **Recipes:** by recipe id (`minecraft:diamond_pickaxe`, KubeJS `recipe:…`, etc.). Vanilla crafting-grid recipes are blocked server-side until unlocked.
- **Whole mod at once:** `@modid` covers items, fluids, and chemicals from that namespace in one stage entry.

Locked items are **auto-dropped** from inventory while the player lacks the stage (tick while online, and on stage removal or reload), so stashing in shulkers does not bypass the lock.

### JEI

With **JEI**, Chapters syncs stages to the client so JEI can hide locked ingredients, hide output recipes for locked items/fluids/chemicals (`TYPE_CHEMICAL` when Mekanism is present), hide recipes by locked recipe id, and show them again after unlock (`includeHidden()` on focus).

### Commands

```
/chapters add <player> <stage>
/chapters remove <player> <stage>
/chapters list <player>
/chapters check <player> <stage>
/chapters reload
```

### Datapack stages

Path:

```
data/<namespace>/chapters/stages/<stage_id>.json
```

Example `data/mypack/chapters/stages/tier1.json`:

```json
{
  "items": [
    "minecraft:netherite_ingot",
    "#minecraft:swords",
    "minecraft:enchanted_book"
  ],
  "fluids": [
    "minecraft:lava",
    "#minecraft:water",
    "@mymodfluids"
  ],
  "chemicals": [
    "mekanism:hydrogen",
    "#mekanism:gases",
    "@mekanism"
  ],
  "recipes": [
    "minecraft:diamond_pickaxe"
  ]
}
```

#### Syntax

| Prefix | Meaning |
| --- | --- |
| `minecraft:foo` | Single id |
| `#namespace:tag` | Tag (items, fluids, chemicals) |
| `@modid` | All items, fluids, and Mekanism chemicals from that mod |

Optional keys: `namespaces`, `fluid_namespaces`, `chemical_namespaces`.

If something appears in **any** stage file, the player needs **at least one** of the stages that list it (OR across definitions that mention the same ingredient), so packs and scripts can layer rules without overwriting each other.

### KubeJS

```js
ServerEvents.loaded(event => {
  ChaptersEvents.defineStage(
    'mypack:tier1',
    [
      'minecraft:netherite_ingot',
      '#minecraft:swords',
      'recipe:minecraft:diamond_pickaxe',
      'fluid:minecraft:lava',
      'chemical:mekanism:hydrogen',
      '@create'
    ]
  )
})
```

- `ChaptersEvents.defineStage(stageId, entries)`. Multiple calls in the same tick batch into one index rebuild.
- `PlayerStages.of(player).add(stageId)` / `.remove(stageId)` / `.has(stageId)` / `.get()`

### Quick start

The jar ships **no** preset stages. Quick test: add a datapack stage gating `minecraft:netherite_ingot`, `/reload`, `/chapters check <you> namespace:stage` (false), try `/give`, then `/chapters add` (true). Sample scripts: [examples/kubejs](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs).

### Compatibility

| Mod | Notes |
| --- | --- |
| NeoForge 26.1.2 (26.1.x) | Required |
| Java 25 | Required |
| JEI | Optional. Hides locked content client-side |
| KubeJS | Optional. Bindings load automatically |
| Mekanism | Optional when a 26.1 build exists; chemical locking disabled in this release |
| FTB Library | Optional. Stage provider for FTB Quests UI |
| FTB Teams | Optional. Shared team stages when Library + Teams are both present |
| FTB Quests | Optional. Stage Reward / Task / Stage Required |

Recipe blocking in the mixin targets the **vanilla crafting grid** (`CraftingMenu`) for now. Other stations may still be hidden in JEI when locked but not blocked server-side the same way.

### Links

- **GitHub:** https://github.com/GabinFqt/chapters
- **Releases:** https://github.com/GabinFqt/chapters/releases
- **License:** MIT

---

## CurseForge

Copy from the next line through the end of this section (CurseForge project description supports Markdown: **#** headings, **bold**, *italic*, lists).

# Chapters

**Documentation (datapacks, KubeJS, FTB, examples):** https://github.com/GabinFqt/chapters/wiki

**Chapters** is a progression mod for **NeoForge 26.1.2**, inspired by *GameStages* and *ItemStages*. Name your progression steps *chapters* (stages) and lock **items**, **fluids**, **Mekanism chemicals**, and **recipes** until you unlock them, so players cannot use gated content early.

Built for pack authors who want **one integrated progression layer** on modern NeoForge: datapacks, KubeJS, JEI, Mekanism, and FTB (Library, Teams, Quests).

## FTB Library, Teams, and Quests

- With **FTB Library**, Chapters becomes the FTB **stage provider**. **FTB Quests** can use **Stage Reward** (grant a chapter when claimed), **Stage Task** (require a chapter), and **Stage Required** on quests or chapters, using your chapter ids with *no extra registration*.
- With **FTB Teams** as well, unlocks live on the **team** and are **shared by the whole party**. Inventory checks and JEI updates apply to all online members. Commands, KubeJS **PlayerStages**, and FTB Quests stage rewards all use the same team storage. Joining a team gives that team's stages; *personal progress does not carry over* the same way (see the wiki for migration notes).
- For **custom conditions** on rewards, pair **FTB Quests Custom Reward** with KubeJS. Sample: *examples/kubejs/server_scripts/ftbquests_chapter_reward.js* in the GitHub repo.

## What you can lock

- **Items:** single id, tag (hash + namespace + path), or *everything from a mod* with at-sign + modid.
- **Fluids:** same rules, including buckets and common transfers when a server player is involved.
- **Mekanism chemicals:** gases, infusions, slurries, pigments by id, tag, or mod (*requires Mekanism*).
- **Recipes:** by recipe id. **Vanilla crafting table** recipes are blocked on the server until unlocked.
- **One mod at once:** at-sign + modid can cover items, fluids, and chemicals from that mod in a single stage entry.
- **Inventory enforcement:** locked items are **dropped automatically** while the player lacks the stage (each tick online, and when stages change or reload).

## JEI

With **JEI** installed, locked ingredients and recipes are **hidden client-side** and return after unlock.

## Commands

Replace *PLAYER* and *STAGE* with real names.

- /chapters add PLAYER STAGE
- /chapters remove PLAYER STAGE
- /chapters list PLAYER
- /chapters check PLAYER STAGE
- /chapters reload

## Datapacks

- Put each stage JSON in your datapack at **data/NAMESPACE/chapters/stages/STAGE_ID.json** (standard Minecraft datapack layout).
- Each file can list **items**, **fluids**, **chemicals**, **recipes**, and optional namespace keys.
- A **plain id** is one entry. A **hash-prefixed** entry selects a **tag**. An **at-sign prefixed** entry selects **everything from that mod** for that category.
- If several stage files mention the same thing, the player needs **at least one** of the stages that reference it.

## KubeJS

- On server loaded, call **ChaptersEvents.defineStage** with a stage id and an array of strings.
- Use the **recipe:**, **fluid:**, and **chemical:** prefixes for non-item entries.
- **PlayerStages.of(player)** supports add, remove, has, and get.
- Multiple **defineStage** calls in the same server tick **batch** into one rebuild.

## Quick start

The jar ships **no** preset stages.

1. Add a datapack stage that locks **netherite ingot**, then **reload**.
2. Run **/chapters check** on yourself: should be *false*.
3. Try **/give** for the item: blocked or dropped.
4. **/chapters add** the stage: item becomes usable.

Example scripts: **examples/kubejs** on [GitHub](https://github.com/GabinFqt/chapters/tree/main/examples/kubejs).

## Compatibility

- **NeoForge 26.1.2** and **Java 25** (*required*).
- **JEI** (*optional*): hides locked content in the recipe viewer.
- **KubeJS** (*optional*): **ChaptersEvents** and **PlayerStages** when present.
- **Mekanism** (*optional* when available for 26.1): chemical locking (not enabled in this release).
- **FTB Library** (*optional*): wires Chapters into FTB Quests stage UI.
- **FTB Teams** (*optional*): **shared team stages** when Library and Teams are both loaded.
- **FTB Quests** (*optional*): Stage Reward, Stage Task, Stage Required.

*Limitation:* server-side recipe blocking currently targets the **vanilla crafting menu** first. Other stations may still differ between JEI and the server; see the **wiki** for details.

## Links

- **GitHub:** https://github.com/GabinFqt/chapters
- **Releases:** https://github.com/GabinFqt/chapters/releases
- **License:** MIT

Issues and suggestions are welcome on GitHub.
