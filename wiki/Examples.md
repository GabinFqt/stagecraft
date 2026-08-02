# Examples

For **NeoForge 26.1.2** + **Chapters**. Pick namespaces to match **your** pack (`mypack`, etc.). **`/reload`** after datapack changes; grant stages with **`/chapters add <player> namespace:stage`**.

[**Tutorial datapack (real files)**](https://github.com/GabinFqt/chapters/tree/main/examples/datapack/tutorial) — same filenames as snippets below where noted.

---

## 1 — One item behind a stage

**Datapack** — `data/tutorial/chapters/stages/intro_nether.json`:

```json
{
  "items": ["minecraft:netherite_ingot"]
}
```

`/chapters check Steve tutorial:intro_nether` → `false`. After `/chapters add Steve tutorial:intro_nether` the ingot works normally again.

**KubeJS:**

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('tutorial:intro_nether', ['minecraft:netherite_ingot'])
})
```

---

## 2 — Three tiers (combine multiple stages)

**Datapack** — three separate files:

`data/tutorial/chapters/stages/tier_early.json`

```json
{ "items": ["minecraft:diamond"] }
```

`data/tutorial/chapters/stages/tier_mid.json`

```json
{ "items": ["minecraft:emerald"] }
```

`data/tutorial/chapters/stages/tier_late.json`

```json
{ "items": ["minecraft:golden_apple"] }
```

With **none** of `tutorial:tier_early` / `tier_mid` / `tier_late`, the player cannot keep any of those items. With **only** `tier_early`, diamond is allowed; emerald and golden apple stay locked until their stages too.

**KubeJS:**

```js
ServerEvents.loaded(() => {
  ChaptersEvents.defineStage('tutorial:tier_early', ['minecraft:diamond'])
  ChaptersEvents.defineStage('tutorial:tier_mid', ['minecraft:emerald'])
  ChaptersEvents.defineStage('tutorial:tier_late', ['minecraft:golden_apple'])
})
```

---

## 3 — Tag vs entire mod

| Goal | In `items` |
| --- | --- |
| All vanilla swords tag | `"#minecraft:swords"` |
| Broad lock for **Create** (items + related fluid/chem rules for `@` — see [[Stages-datapack]]) | `"@create"` |

**Datapack:**

```json
{
  "items": ["#minecraft:swords", "@create"]
}
```

**KubeJS:**

```js
ChaptersEvents.defineStage('mypack:gated', ['#minecraft:swords', '@create'])
```

Tighter fluid-only or chemical-only extras: use `fluid_namespaces` / `chemical_namespaces` in JSON as in [[Stages-datapack]].

---

## 4 — Water and lava

**Datapack** — `data/tutorial/chapters/stages/fluides_base.json` → stage **`tutorial:fluides_base`**

```json
{
  "fluids": [
    "minecraft:water",
    "minecraft:lava"
  ]
}
```

**KubeJS:**

```js
ChaptersEvents.defineStage('tutorial:fluides_base', [
  'fluid:minecraft:water',
  'fluid:minecraft:lava'
])
```

---

## 5 — Lock one recipe

Vanilla grid behaviour + JEI: see [[JEI-and-limitations]].

**Datapack:**

```json
{
  "recipes": ["minecraft:diamond_pickaxe"]
}
```

**KubeJS:**

```js
ChaptersEvents.defineStage('tutorial:recipe_pickaxe', [
  'recipe:minecraft:diamond_pickaxe'
])
```

---

## 6 — Mekanism hydrogen (optional)

No Mekanism → lines are accepted but **no chemicals** are indexed.

**Datapack:**

```json
{
  "chemicals": ["mekanism:hydrogen"]
}
```

**KubeJS:**

```js
ChaptersEvents.defineStage('tutorial:mek_h2', ['chemical:mekanism:hydrogen'])
```

Fully automated Mekanism pipe transfers can behave differently from transfers you do manually in GUIs — factor that into hardcore packs.

---

## Try it locally

Paste [**examples/kubejs/server_scripts/main.js**](https://github.com/GabinFqt/chapters/blob/main/examples/kubejs/server_scripts/main.js) into **`kubejs/server_scripts/`** for a louder default sandbox ([**examples readme**](https://github.com/GabinFqt/chapters/tree/main/examples)).
