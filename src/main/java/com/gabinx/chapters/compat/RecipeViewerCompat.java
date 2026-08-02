package com.gabinx.chapters.compat;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.jei.ChaptersJeiPlugin;
import com.gabinx.chapters.stage.ClientStageCache;
import com.gabinx.chapters.stage.ClientStageIndices;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class RecipeViewerCompat {
    private RecipeViewerCompat() {
    }

    public static void refresh() {
        Set<Identifier> lockedFluids = getLockedFluidIds();
        Set<Identifier> lockedItems = getLockedItemIds(lockedFluids);
        Set<Identifier> lockedChemicals = getLockedChemicalIds();
        Set<Identifier> lockedRecipes = getLockedRecipeIds();
        Chapters.LOGGER.debug(
                "Recipe viewer refresh — locked items: {}, locked fluids: {}, locked chemicals: {}, locked recipes: {}",
                lockedItems.size(),
                lockedFluids.size(),
                lockedChemicals.size(),
                lockedRecipes.size()
        );
        if (ModList.get().isLoaded("jei")) {
            ChaptersJeiPlugin.onLockedIngredientsChanged(lockedItems, lockedFluids, lockedChemicals, lockedRecipes);
        }
    }

    /**
     * Items the client-side player cannot use (mirrors {@link com.gabinx.chapters.stage.LockResolver}: need at least one
     * stage among every definition that mentions the stack), plus filled buckets whose fluid is locked.
     */
    private static Set<Identifier> getLockedItemIds(Set<Identifier> lockedFluids) {
        Set<Identifier> locked = computeLocked(ClientStageIndices.itemsView());
        for (Identifier fluidId : lockedFluids) {
            Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                continue;
            }
            Item bucket = fluid.getBucket();
            if (bucket == null || new ItemStack(bucket).isEmpty()) {
                continue;
            }
            Identifier bucketId = BuiltInRegistries.ITEM.getKey(bucket);
            if (bucketId != null) {
                locked.add(bucketId);
            }
        }
        return locked;
    }

    private static Set<Identifier> getLockedFluidIds() {
        return computeLocked(ClientStageIndices.fluidsView());
    }

    private static Set<Identifier> getLockedChemicalIds() {
        return computeLocked(ClientStageIndices.chemicalsView());
    }

    private static Set<Identifier> getLockedRecipeIds() {
        return computeLocked(ClientStageIndices.recipesView());
    }

    private static Set<Identifier> computeLocked(Map<Identifier, Set<Identifier>> index) {
        Set<Identifier> activeStages = ClientStageCache.snapshot();
        Set<Identifier> locked = new LinkedHashSet<>();
        for (Map.Entry<Identifier, Set<Identifier>> entry : index.entrySet()) {
            Identifier id = entry.getKey();
            Set<Identifier> definingStages = entry.getValue();
            boolean unlocked = false;
            for (Identifier required : definingStages) {
                if (activeStages.contains(required)) {
                    unlocked = true;
                    break;
                }
            }
            if (!unlocked) {
                locked.add(id);
            }
        }
        return locked;
    }
}
