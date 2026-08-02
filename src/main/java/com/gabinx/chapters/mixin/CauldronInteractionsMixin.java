package com.gabinx.chapters.mixin;

import com.gabinx.chapters.stage.LockResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(CauldronInteractions.class)
public abstract class CauldronInteractionsMixin {
    @Inject(method = "fillBucket", at = @At("HEAD"), cancellable = true)
    private static void chapters$gateCauldronFill(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        ItemStack itemInHand,
        ItemStack newItem,
        Predicate<BlockState> canFill,
        SoundEvent soundEvent,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!(newItem.getItem() instanceof BucketItem bucket)) {
            return;
        }
        Fluid fluid = bucket.content;
        if (fluid != null && fluid != Fluids.EMPTY && LockResolver.isFluidLocked(player, fluid)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "emptyBucket", at = @At("HEAD"), cancellable = true)
    private static void chapters$gateCauldronEmpty(
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        ItemStack itemInHand,
        BlockState newState,
        SoundEvent soundEvent,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!(itemInHand.getItem() instanceof BucketItem bucket)) {
            return;
        }
        Fluid fluid = bucket.content;
        if (fluid != null && fluid != Fluids.EMPTY && LockResolver.isFluidLocked(player, fluid)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
