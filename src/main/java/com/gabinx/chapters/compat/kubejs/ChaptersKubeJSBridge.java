package com.gabinx.chapters.compat.kubejs;

import com.gabinx.chapters.stage.StageDefinition;
import com.gabinx.chapters.stage.StageManager;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChaptersKubeJSBridge {
    private static final Map<Identifier, Collection<String>> RAW = new ConcurrentHashMap<>();
    private static volatile boolean flushScheduled;

    private ChaptersKubeJSBridge() {
    }

    public static void defineStage(Identifier id, Collection<String> entries) {
        RAW.put(id, new ArrayList<>(entries));
        flushScheduled = true;
    }

    public static void clearStage(Identifier id) {
        RAW.remove(id);
        flushScheduled = true;
    }

    /**
     * Flushes deferred stage definitions once per server tick so scripts can call {@link #defineStage} many times in one
     * {@code ServerEvents.loaded} callback without rebuilding indices repeatedly.
     */
    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (!flushScheduled) {
            return;
        }
        flushScheduled = false;
        flushToStageManager();
    }

    private static void flushToStageManager() {
        var defs = new ArrayList<StageDefinition>();
        for (var entry : RAW.entrySet()) {
            Identifier id = entry.getKey();
            var items = new LinkedHashSet<Identifier>();
            var tags = new LinkedHashSet<TagKey<Item>>();
            var namespaces = new LinkedHashSet<String>();
            var fluids = new LinkedHashSet<Identifier>();
            var fluidTags = new LinkedHashSet<TagKey<Fluid>>();
            var fluidNamespaces = new LinkedHashSet<String>();
            var chemicals = new LinkedHashSet<Identifier>();
            var chemicalTags = new LinkedHashSet<Identifier>();
            var chemicalNamespaces = new LinkedHashSet<String>();
            var recipes = new LinkedHashSet<Identifier>();

            for (String raw : entry.getValue()) {
                if (raw != null && raw.regionMatches(true, 0, "fluid:", 0, 6)) {
                    StageDefinition.accumulateFluidEntry(raw.substring(6).trim(), fluids, fluidTags, fluidNamespaces);
                } else if (raw != null && raw.regionMatches(true, 0, "chemical:", 0, 9)) {
                    StageDefinition.accumulateChemicalEntry(raw.substring(9).trim(), chemicals, chemicalTags, chemicalNamespaces);
                } else if (raw != null && raw.regionMatches(true, 0, "recipe:", 0, 7)) {
                    StageDefinition.accumulateRecipeEntry(raw.substring(7).trim(), recipes);
                } else {
                    StageDefinition.accumulateEntry(raw, items, tags, namespaces);
                    // Match items: `@mod_id` gates every fluid and Mekanism chemical in that namespace too.
                    if (raw != null && raw.trim().startsWith("@")) {
                        StageDefinition.accumulateFluidEntry(raw.trim(), fluids, fluidTags, fluidNamespaces);
                        StageDefinition.accumulateChemicalEntry(raw.trim(), chemicals, chemicalTags, chemicalNamespaces);
                    }
                }
            }

            defs.add(new StageDefinition(
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
                    recipes));
        }

        StageManager.get().setRuntimeDefinitions(defs);
    }
}
