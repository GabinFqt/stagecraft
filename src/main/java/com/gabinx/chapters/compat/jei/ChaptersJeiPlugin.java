package com.gabinx.chapters.compat.jei;

import net.minecraft.resources.Identifier;

import java.util.Set;

public final class ChaptersJeiPlugin {
    private ChaptersJeiPlugin() {
    }

    public static void onLockedIngredientsChanged(
            Set<Identifier> lockedItems,
            Set<Identifier> lockedFluids,
            Set<Identifier> lockedChemicals,
            Set<Identifier> lockedRecipes
    ) {
        ChaptersJeiModPlugin.applyLocked(lockedItems, lockedFluids, lockedChemicals, lockedRecipes);
    }
}
