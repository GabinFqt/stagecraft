package com.gabinx.chapters.mixin;

import com.gabinx.chapters.stage.LockResolver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gates vanilla bucket world pickup and place when no dedicated fill-bucket event is available.
 * Pickup goes through {@link BucketPickup#pickupBlock} on a {@link LevelAccessor}.
 */
@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Shadow
    @Final
    public Fluid content;

    /** Cancel filled-bucket place attempts early (client prediction + server). */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void chapters$gateBucketUse(
        Level level,
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (content != null && content != Fluids.EMPTY && LockResolver.isFluidLocked(player, content)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    /**
     * Hard gate at the moment vanilla would remove the fluid. Returning empty makes {@link BucketItem#use} fail.
     */
    @WrapOperation(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack chapters$gateBucketPickup(
        BucketPickup instance,
        LivingEntity user,
        LevelAccessor level,
        BlockPos pos,
        BlockState state,
        Operation<ItemStack> original
    ) {
        if (user instanceof Player player) {
            Fluid fluid = state.getFluidState().getType();
            if (fluid != Fluids.EMPTY && LockResolver.isFluidLocked(player, fluid)) {
                return ItemStack.EMPTY;
            }
        }
        return original.call(instance, user, level, pos, state);
    }

    @Inject(
        method = "emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void chapters$gateBucketPlace(
        @Nullable LivingEntity user,
        Level level,
        BlockPos pos,
        @Nullable BlockHitResult hitResult,
        @Nullable ItemStack containerItem,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(user instanceof Player player)) {
            return;
        }
        if (content == null || content == Fluids.EMPTY) {
            return;
        }
        if (LockResolver.isFluidLocked(player, content)) {
            cir.setReturnValue(false);
        }
    }
}
