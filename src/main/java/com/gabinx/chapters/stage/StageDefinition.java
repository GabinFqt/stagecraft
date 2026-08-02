package com.gabinx.chapters.stage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record StageDefinition(
        Identifier id,
        Set<Identifier> items,
        Set<TagKey<Item>> tags,
        Set<String> namespaces,
        Set<Identifier> fluids,
        Set<TagKey<Fluid>> fluidTags,
        Set<String> fluidNamespaces,
        Set<Identifier> chemicals,
        Set<Identifier> chemicalTags,
        Set<String> chemicalNamespaces,
        /** Crafting (and other) recipe holder ids to lock until the player has this stage. */
        Set<Identifier> recipes) {

    public static StageDefinition fromJson(Identifier id, JsonObject json) {
        Set<Identifier> items = new LinkedHashSet<>();
        Set<TagKey<Item>> tags = new LinkedHashSet<>();
        Set<String> namespaces = new LinkedHashSet<>();
        Set<Identifier> fluids = new LinkedHashSet<>();
        Set<TagKey<Fluid>> fluidTags = new LinkedHashSet<>();
        Set<String> fluidNamespaces = new LinkedHashSet<>();
        Set<Identifier> chemicals = new LinkedHashSet<>();
        Set<Identifier> chemicalTags = new LinkedHashSet<>();
        Set<String> chemicalNamespaces = new LinkedHashSet<>();
        Set<Identifier> recipes = new LinkedHashSet<>();

        JsonArray namespacesJson = json.getAsJsonArray("namespaces");
        if (namespacesJson != null) {
            for (JsonElement element : namespacesJson) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                String n = element.getAsString();
                addNamespace(namespaces, n);
                addFluidNamespace(fluidNamespaces, n);
                addChemicalNamespace(chemicalNamespaces, n);
            }
        }

        JsonArray fluidNamespacesJson = json.getAsJsonArray("fluid_namespaces");
        if (fluidNamespacesJson != null) {
            for (JsonElement element : fluidNamespacesJson) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                addFluidNamespace(fluidNamespaces, element.getAsString());
            }
        }

        JsonArray chemicalNamespacesJson = json.getAsJsonArray("chemical_namespaces");
        if (chemicalNamespacesJson != null) {
            for (JsonElement element : chemicalNamespacesJson) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                addChemicalNamespace(chemicalNamespaces, element.getAsString());
            }
        }

        JsonArray values = json.getAsJsonArray("items");
        if (values != null) {
            for (JsonElement element : values) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                String itemEntry = element.getAsString();
                accumulateEntry(itemEntry, items, tags, namespaces);
                if (itemEntry != null && itemEntry.trim().startsWith("@")) {
                    accumulateFluidEntry(itemEntry.trim(), fluids, fluidTags, fluidNamespaces);
                    accumulateChemicalEntry(itemEntry.trim(), chemicals, chemicalTags, chemicalNamespaces);
                }
            }
        }

        JsonArray fluidValues = json.getAsJsonArray("fluids");
        if (fluidValues != null) {
            for (JsonElement element : fluidValues) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                accumulateFluidEntry(element.getAsString(), fluids, fluidTags, fluidNamespaces);
            }
        }

        JsonArray chemicalValues = json.getAsJsonArray("chemicals");
        if (chemicalValues != null) {
            for (JsonElement element : chemicalValues) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                accumulateChemicalEntry(element.getAsString(), chemicals, chemicalTags, chemicalNamespaces);
            }
        }

        JsonArray recipeValues = json.getAsJsonArray("recipes");
        if (recipeValues != null) {
            for (JsonElement element : recipeValues) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                accumulateRecipeEntry(element.getAsString(), recipes);
            }
        }

        return new StageDefinition(
                id,
                items,
                tags,
                namespaces,
                fluids,
                fluidTags,
                fluidNamespaces,
                chemicals,
                chemicalTags,
                chemicalNamespaces,
                recipes);
    }

    /**
     * One stage-list entry from a datapack or KubeJS: item id (namespaced path), {@code #item_tag}, or
     * {@code @mod_id} meaning every item registered under that namespace.
     */
    public static void accumulateEntry(
            String raw,
            Set<Identifier> itemsOut,
            Set<TagKey<Item>> tagsOut,
            Set<String> namespacesOut) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(trimmed.substring(1).trim());
            if (tagId != null) {
                tagsOut.add(TagKey.create(BuiltInRegistries.ITEM.key(), tagId));
            }
            return;
        }
        if (trimmed.startsWith("@")) {
            addNamespace(namespacesOut, trimmed.substring(1));
            return;
        }
        Identifier itemId = Identifier.tryParse(trimmed);
        if (itemId != null) {
            itemsOut.add(itemId);
        }
    }

    /**
     * {@code namespaces} datapack column (and optional {@code @} prefix); same validation as Vanilla mod ids.
     */
    static void addNamespace(Set<String> namespacesOut, String raw) {
        if (raw == null) {
            return;
        }
        String s = raw.trim();
        if (s.startsWith("@")) {
            s = s.substring(1).trim();
        }
        if (s.isEmpty()) {
            return;
        }
        try {
            Identifier.fromNamespaceAndPath(s, "x");
            namespacesOut.add(s);
        } catch (IllegalArgumentException ignored) {
        }
    }

    static void addFluidNamespace(Set<String> namespacesOut, String raw) {
        addNamespace(namespacesOut, raw);
    }

    static void addChemicalNamespace(Set<String> namespacesOut, String raw) {
        addNamespace(namespacesOut, raw);
    }

    /**
     * One stage-list fluid entry from a datapack or KubeJS: fluid id, {@code #fluid_tag}, or {@code @} mod namespace for
     * every fluid registered under that namespace.
     */
    public static void accumulateFluidEntry(
            String raw,
            Set<Identifier> fluidsOut,
            Set<TagKey<Fluid>> fluidTagsOut,
            Set<String> fluidNamespacesOut) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(trimmed.substring(1).trim());
            if (tagId != null) {
                fluidTagsOut.add(TagKey.create(BuiltInRegistries.FLUID.key(), tagId));
            }
            return;
        }
        if (trimmed.startsWith("@")) {
            addFluidNamespace(fluidNamespacesOut, trimmed.substring(1));
            return;
        }
        Identifier fluidId = Identifier.tryParse(trimmed);
        if (fluidId != null) {
            fluidsOut.add(fluidId);
        }
    }

    /**
     * Mekanism chemical id, {@code #chemical_tag} on Mekanism's chemical registry, or {@code @} mod namespace (requires
     * Mekanism at runtime to expand).
     */
    public static void accumulateChemicalEntry(
            String raw,
            Set<Identifier> chemicalsOut,
            Set<Identifier> chemicalTagIdsOut,
            Set<String> chemicalNamespacesOut) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(trimmed.substring(1).trim());
            if (tagId != null) {
                chemicalTagIdsOut.add(tagId);
            }
            return;
        }
        if (trimmed.startsWith("@")) {
            addChemicalNamespace(chemicalNamespacesOut, trimmed.substring(1));
            return;
        }
        Identifier chemicalId = Identifier.tryParse(trimmed);
        if (chemicalId != null) {
            chemicalsOut.add(chemicalId);
        }
    }

    /**
     * One stage-list recipe entry: exact recipe id ({@code minecraft:stick}).
     */
    public static void accumulateRecipeEntry(String raw, Set<Identifier> recipesOut) {
        if (raw == null) {
            return;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        Identifier recipeId = Identifier.tryParse(trimmed);
        if (recipeId != null) {
            recipesOut.add(recipeId);
        }
    }

    public boolean matches(ItemStack stack) {
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (items.contains(key)) {
            return true;
        }
        if (!namespaces.isEmpty() && namespaces.contains(key.getNamespace())) {
            return true;
        }
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Still and flowing variants ({@code minecraft:water} vs {@code minecraft:flowing_water}, NeoForge
     * {@code minecraft:milk} vs {@code minecraft:flowing_milk}) share one logical fluid for stages; match on the
     * source fluid id like filled buckets do.
     */
    public static Identifier fluidKindRegistryKey(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowing) {
            return BuiltInRegistries.FLUID.getKey(flowing.getSource());
        }
        return BuiltInRegistries.FLUID.getKey(fluid);
    }

    public boolean matchesFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Identifier kindKey = fluidKindRegistryKey(stack.getFluid());
        if (kindKey != null && fluids.contains(kindKey)) {
            return true;
        }
        if (kindKey != null && !fluidNamespaces.isEmpty() && fluidNamespaces.contains(kindKey.getNamespace())) {
            return true;
        }
        for (TagKey<Fluid> tag : fluidTags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public StageDefinition {
        Objects.requireNonNull(id, "id");
        items = Set.copyOf(items);
        tags = Set.copyOf(tags);
        namespaces = Set.copyOf(namespaces);
        fluids = Set.copyOf(fluids);
        fluidTags = Set.copyOf(fluidTags);
        fluidNamespaces = Set.copyOf(fluidNamespaces);
        chemicals = Set.copyOf(chemicals);
        chemicalTags = Set.copyOf(chemicalTags);
        chemicalNamespaces = Set.copyOf(chemicalNamespaces);
        recipes = Set.copyOf(recipes);
    }
}
