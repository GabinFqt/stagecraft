package com.gabinx.chapters.compat.kubejs.bindings;

import com.gabinx.chapters.compat.kubejs.ChaptersKubeJSBridge;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;

public final class ChaptersEventsBinding {
    /**
     * Entries match datapack-style item lines ({@code minecraft:apple}, {@code #tag}, {@code @mod_id}).
     * A leading {@code @} also applies to every fluid and every Mekanism chemical registered under that namespace (same as datapack {@code namespaces}).
     * Optional {@code fluid:} prefix for fluids only ({@code fluid:minecraft:lava}, {@code fluid:#c:water}, {@code fluid:@mod_id}).
     * Optional {@code chemical:} prefix for Mekanism chemicals only ({@code chemical:mekanism:hydrogen}, {@code chemical:#mekanism:gases}, {@code chemical:@mekanism}) when Mekanism is installed.
     * Optional {@code recipe:} prefix locks a recipe by id only ({@code recipe:minecraft:diamond_pickaxe}); the output item can stay visible if not otherwise locked.
     * <p>
     * Breaking change: use a single collection-form call from KubeJS:
     * {@code defineStage('namespace:stage_id', ['minecraft:apple', '#minecraft:axes'])}.
     */
    public void defineStage(String id, Collection<?> locks) {
        Identifier stageId = Identifier.tryParse(id);
        if (stageId == null || locks == null) {
            return;
        }

        var entries = new ArrayList<String>(locks.size());
        for (Object lock : locks) {
            if (lock != null) {
                entries.add(lock.toString());
            }
        }

        ChaptersKubeJSBridge.defineStage(stageId, entries);
    }

    public void clearStage(String id) {
        Identifier stageId = Identifier.tryParse(id);
        if (stageId != null) {
            ChaptersKubeJSBridge.clearStage(stageId);
        }
    }
}
