package com.gabinx.chapters.stage;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Server-authoritative snapshots of the four stage-locking indices, mirrored on the logical client so that
 * {@link com.gabinx.chapters.compat.RecipeViewerCompat} can compute locked sets without depending on
 * {@link StageManager}'s own indices (which are only populated on the server thread by datapack /
 * KubeJS merge).
 * <p>
 * Without this, dedicated-server clients see empty indices and JEI hides nothing; only integrated
 * servers (singleplayer / open-to-LAN) work because they share the JVM with the server-side
 * {@link StageManager} singleton.
 */
public final class ClientStageIndices {
    private static volatile Map<Identifier, Set<Identifier>> items = Map.of();
    private static volatile Map<Identifier, Set<Identifier>> fluids = Map.of();
    private static volatile Map<Identifier, Set<Identifier>> chemicals = Map.of();
    private static volatile Map<Identifier, Set<Identifier>> recipes = Map.of();

    private ClientStageIndices() {
    }

    public static synchronized void replace(
            Map<Identifier, Set<Identifier>> nextItems,
            Map<Identifier, Set<Identifier>> nextFluids,
            Map<Identifier, Set<Identifier>> nextChemicals,
            Map<Identifier, Set<Identifier>> nextRecipes
    ) {
        items = freeze(nextItems);
        fluids = freeze(nextFluids);
        chemicals = freeze(nextChemicals);
        recipes = freeze(nextRecipes);
    }

    public static Map<Identifier, Set<Identifier>> itemsView() {
        return items;
    }

    public static Map<Identifier, Set<Identifier>> fluidsView() {
        return fluids;
    }

    public static Map<Identifier, Set<Identifier>> chemicalsView() {
        return chemicals;
    }

    public static Map<Identifier, Set<Identifier>> recipesView() {
        return recipes;
    }

    private static Map<Identifier, Set<Identifier>> freeze(
            Map<Identifier, Set<Identifier>> next
    ) {
        if (next == null || next.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, Set<Identifier>> copy = new LinkedHashMap<>(next.size());
        for (Map.Entry<Identifier, Set<Identifier>> e : next.entrySet()) {
            copy.put(e.getKey(), Set.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }
}
