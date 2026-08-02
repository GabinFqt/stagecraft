package com.gabinx.chapters.stage;

import com.gabinx.chapters.compat.ftb.EffectiveStages;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Set;

public final class LockResolver {
    private LockResolver() {
    }

    public static boolean isLocked(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (StageManager.get().isItemLocked(EffectiveStages.snapshot(player), stack)) {
            return true;
        }
        // Filled buckets are gated by their fluid stage (water_bucket → water, lava_bucket → lava).
        return isFluidBucketLocked(player, stack);
    }

    public static boolean isFluidLocked(ServerPlayer player, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return StageManager.get().isFluidLocked(EffectiveStages.snapshot(player), stack);
    }

    /**
     * Server uses {@link EffectiveStages} + {@link StageManager}; client prediction uses the mirrored
     * {@link ClientStageIndices} / {@link ClientStageCache} so empty-bucket fill does not visually succeed.
     */
    public static boolean isFluidLocked(Player player, Fluid fluid) {
        if (player == null || fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        FluidStack stack = new FluidStack(fluid, 1000);
        if (stack.isEmpty()) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer && !player.level().isClientSide()) {
            return isFluidLocked(serverPlayer, stack);
        }
        if (player.level().isClientSide()) {
            return isFluidLockedClient(stack);
        }
        return false;
    }

    /** True when this stack is a filled bucket whose fluid is locked for the player. */
    public static boolean isFluidBucketLocked(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BucketItem bucket)) {
            return false;
        }
        Fluid filled = bucket.content;
        if (filled == null || filled == Fluids.EMPTY) {
            return false;
        }
        return isFluidLocked(player, filled);
    }

    private static boolean isFluidLockedClient(FluidStack stack) {
        Identifier kind = StageDefinition.fluidKindRegistryKey(stack.getFluid());
        if (kind == null) {
            return false;
        }
        Set<Identifier> defining = ClientStageIndices.fluidsView().get(kind);
        if (defining == null || defining.isEmpty()) {
            return false;
        }
        Set<Identifier> active = ClientStageCache.snapshot();
        for (Identifier stageId : defining) {
            if (active.contains(stageId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Mekanism chemical registry key (e.g. {@code mekanism:hydrogen}). Only meaningful when Mekanism is installed.
     */
    public static boolean isChemicalLocked(ServerPlayer player, Identifier chemicalRegistryKey) {
        if (chemicalRegistryKey == null) {
            return false;
        }

        return StageManager.get().isChemicalLocked(EffectiveStages.snapshot(player), chemicalRegistryKey);
    }

    public static boolean isRecipeLocked(ServerPlayer player, Identifier recipeHolderId) {
        if (recipeHolderId == null) {
            return false;
        }

        return StageManager.get().isRecipeLocked(EffectiveStages.snapshot(player), recipeHolderId);
    }
}
