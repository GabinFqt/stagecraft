package com.gabinx.chapters.mixin;

import com.gabinx.chapters.stage.ChemicalInteractionActor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(
        method = "clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V",
        at = @At("HEAD")
    )
    private void chapters$beginChemicalActor(int slotId, int button, ContainerInput containerInput, Player player, CallbackInfo ci) {
        ChemicalInteractionActor.set(player);
    }

    @Inject(
        method = "clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V",
        at = @At("RETURN")
    )
    private void chapters$endChemicalActor(int slotId, int button, ContainerInput containerInput, Player player, CallbackInfo ci) {
        ChemicalInteractionActor.clear();
    }
}
