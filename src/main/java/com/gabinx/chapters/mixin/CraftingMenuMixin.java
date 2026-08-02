package com.gabinx.chapters.mixin;

import com.gabinx.chapters.stage.LockResolver;

import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(
        method = "slotChangedCraftingGrid",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
        cancellable = true,
        // Do not capture Optional<RecipeHolder<?>> here: other mods’ transformers often drop that local, which breaks
        // CAPTURE_FAILHARD. We only need CraftingInput, ServerPlayer, and the computed result stack.
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void chapters$hideLockedResult(
        AbstractContainerMenu menu,
        ServerLevel level,
        net.minecraft.world.entity.player.Player player,
        CraftingContainer craftSlots,
        ResultContainer resultSlots,
        @Nullable RecipeHolder<CraftingRecipe> recipe,
        CallbackInfo ci,
        CraftingInput input,
        ServerPlayer serverPlayer,
        ItemStack result
    ) {
        Optional<RecipeHolder<CraftingRecipe>> resolved = level.getServer()
                .getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level, recipe);
        Identifier recipeId = resolved.map(holder -> holder.id().identifier()).orElse(null);
        boolean outputLocked = !result.isEmpty() && LockResolver.isLocked(serverPlayer, result);
        boolean recipeLocked = recipeId != null && LockResolver.isRecipeLocked(serverPlayer, recipeId);
        if (!(outputLocked || recipeLocked)) {
            return;
        }

        resultSlots.setItem(0, ItemStack.EMPTY);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, ItemStack.EMPTY));
        ci.cancel();
    }
}
