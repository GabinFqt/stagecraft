package com.gabinx.chapters.event;

import com.gabinx.chapters.stage.LockResolver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

public final class PickupHandler {
    private PickupHandler() {
    }

    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (LockResolver.isLocked(player, event.getItemEntity().getItem())) {
            event.setCanPickup(TriState.FALSE);
        }
    }
}
