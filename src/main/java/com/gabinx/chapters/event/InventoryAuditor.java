package com.gabinx.chapters.event;

import com.gabinx.chapters.stage.LockResolver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class InventoryAuditor {
    private InventoryAuditor() {
    }

    /**
     * Periodically drops locked items (e.g. after /give, loot, or stages changed without going through
     * {@link com.gabinx.chapters.api.ChaptersAPI#removeStage}).
     */
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            auditNow(player);
        }
    }

    public static void auditNow(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!LockResolver.isLocked(player, stack)) {
                continue;
            }

            ItemStack drop = stack.copy();
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            player.drop(drop, false);
        }
    }
}
