package com.gabinx.chapters.mixin.compat.mekanism;

import com.gabinx.chapters.stage.ChemicalInteractionActor;
import com.gabinx.chapters.stage.LockResolver;
import mekanism.api.AutomationType;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalUtils;
import mekanism.api.container.InContainerGetter;
import mekanism.api.container.ContainerInteraction;
import mekanism.api.container.LongContainerInteraction;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.ToIntFunction;

@Mixin(value = ChemicalUtils.class, remap = false)
public abstract class ChemicalUtilsMixin {
    private static final ThreadLocal<Integer> CHAPTERS_CHEMICAL_RECURSION = ThreadLocal.withInitial(() -> 0);

    private static boolean bypassAutomation(@Nullable AutomationType automationType) {
        return automationType == AutomationType.EXTERNAL;
    }

    private static boolean shouldGate(@Nullable AutomationType automationType) {
        Player p = ChemicalInteractionActor.get();
        return p instanceof ServerPlayer && !bypassAutomation(automationType);
    }

    private static boolean isLocked(ServerPlayer player, ChemicalStack stack) {
        Identifier key = MekanismAPI.CHEMICAL_REGISTRY.getKey(stack.getChemical());
        return key != null && LockResolver.isChemicalLocked(player, key);
    }

    @Inject(
        method = "insert(Lmekanism/api/chemical/ChemicalStack;Lnet/minecraft/core/Direction;Lmekanism/api/Action;"
            + "Ljava/util/function/ToIntFunction;Lmekanism/api/container/InContainerGetter;Lmekanism/api/container/ContainerInteraction;)Lmekanism/api/chemical/ChemicalStack;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void chapters$gateInsertHandler(
        ChemicalStack stack,
        Direction side,
        Action action,
        ToIntFunction<Direction> tankCount,
        InContainerGetter<ChemicalStack> inTankGetter,
        ContainerInteraction<ChemicalStack> insertChemical,
        CallbackInfoReturnable<ChemicalStack> cir
    ) {
        if (stack.isEmpty()) {
            return;
        }
        if (!shouldGate(null)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer) ChemicalInteractionActor.get();
        if (isLocked(sp, stack)) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(
        method = "insert(Lmekanism/api/chemical/ChemicalStack;Lmekanism/api/Action;Lmekanism/api/AutomationType;ILjava/util/List;)Lmekanism/api/chemical/ChemicalStack;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void chapters$gateInsertTanks(
        ChemicalStack stack,
        Action action,
        AutomationType automationType,
        int size,
        java.util.List chemicalTanks,
        CallbackInfoReturnable<ChemicalStack> cir
    ) {
        if (stack.isEmpty()) {
            return;
        }
        if (!shouldGate(automationType)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer) ChemicalInteractionActor.get();
        if (isLocked(sp, stack)) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(
        method = "extract(JLnet/minecraft/core/Direction;Lmekanism/api/Action;Ljava/util/function/ToIntFunction;"
            + "Lmekanism/api/container/InContainerGetter;Lmekanism/api/container/LongContainerInteraction;)Lmekanism/api/chemical/ChemicalStack;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void chapters$gateExtractLongHandler(
        long amount,
        Direction side,
        Action action,
        ToIntFunction<Direction> tankCount,
        InContainerGetter<ChemicalStack> inTankGetter,
        LongContainerInteraction extractChemical,
        CallbackInfoReturnable<ChemicalStack> cir
    ) {
        if (amount == 0) {
            return;
        }
        if (!shouldGate(null)) {
            return;
        }
        int depth = CHAPTERS_CHEMICAL_RECURSION.get();
        if (depth > 0) {
            return;
        }
        CHAPTERS_CHEMICAL_RECURSION.set(depth + 1);
        try {
            ChemicalStack trial = ChemicalUtils.extract(amount, side, Action.SIMULATE, tankCount, inTankGetter, extractChemical);
            ServerPlayer sp = (ServerPlayer) ChemicalInteractionActor.get();
            if (!trial.isEmpty() && isLocked(sp, trial)) {
                cir.setReturnValue(ChemicalStack.EMPTY);
            }
        } finally {
            CHAPTERS_CHEMICAL_RECURSION.set(depth);
        }
    }

    @Inject(
        method = "extract(JLmekanism/api/Action;Lmekanism/api/AutomationType;ILjava/util/List;)Lmekanism/api/chemical/ChemicalStack;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void chapters$gateExtractLongTanks(
        long amount,
        Action action,
        AutomationType automationType,
        int size,
        java.util.List chemicalTanks,
        CallbackInfoReturnable<ChemicalStack> cir
    ) {
        if (amount == 0) {
            return;
        }
        if (!shouldGate(automationType)) {
            return;
        }
        int depth = CHAPTERS_CHEMICAL_RECURSION.get();
        if (depth > 0) {
            return;
        }
        CHAPTERS_CHEMICAL_RECURSION.set(depth + 1);
        try {
            ChemicalStack trial = ChemicalUtils.extract(amount, action, automationType, size, chemicalTanks);
            ServerPlayer sp = (ServerPlayer) ChemicalInteractionActor.get();
            if (!trial.isEmpty() && isLocked(sp, trial)) {
                cir.setReturnValue(ChemicalStack.EMPTY);
            }
        } finally {
            CHAPTERS_CHEMICAL_RECURSION.set(depth);
        }
    }

    @Inject(
        method = "extract(Lmekanism/api/chemical/ChemicalStack;Lnet/minecraft/core/Direction;Lmekanism/api/Action;Ljava/util/function/ToIntFunction;"
            + "Lmekanism/api/container/InContainerGetter;Lmekanism/api/container/LongContainerInteraction;)Lmekanism/api/chemical/ChemicalStack;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void chapters$gateExtractStackHandler(
        ChemicalStack stack,
        Direction side,
        Action action,
        ToIntFunction<Direction> tankCount,
        InContainerGetter<ChemicalStack> inTankGetter,
        LongContainerInteraction extractChemical,
        CallbackInfoReturnable<ChemicalStack> cir
    ) {
        if (stack.isEmpty()) {
            return;
        }
        if (!shouldGate(null)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer) ChemicalInteractionActor.get();
        if (isLocked(sp, stack)) {
            cir.setReturnValue(ChemicalStack.EMPTY);
        }
    }

    @Inject(
        method = "extract(Lmekanism/api/chemical/ChemicalStack;Lmekanism/api/Action;Lmekanism/api/AutomationType;ILjava/lang/Iterable;)Lmekanism/api/chemical/ChemicalStack;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void chapters$gateExtractStackTanks(
        ChemicalStack stack,
        Action action,
        AutomationType automationType,
        int size,
        Iterable chemicalTanks,
        CallbackInfoReturnable<ChemicalStack> cir
    ) {
        if (stack.isEmpty()) {
            return;
        }
        if (!shouldGate(automationType)) {
            return;
        }
        ServerPlayer sp = (ServerPlayer) ChemicalInteractionActor.get();
        if (isLocked(sp, stack)) {
            cir.setReturnValue(ChemicalStack.EMPTY);
        }
    }
}
