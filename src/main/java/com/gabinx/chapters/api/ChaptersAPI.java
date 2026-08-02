package com.gabinx.chapters.api;

import com.gabinx.chapters.compat.ftb.EffectiveStages;
import com.gabinx.chapters.event.InventoryAuditor;
import com.gabinx.chapters.network.ClientboundStageDeltaPayload;
import com.gabinx.chapters.network.ClientboundStageIndicesPayload;
import com.gabinx.chapters.network.ClientboundStagesPayload;
import com.gabinx.chapters.stage.StageDefinition;
import com.gabinx.chapters.stage.StageManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.Set;

public final class ChaptersAPI {
    private ChaptersAPI() {
    }

    public static boolean addStage(ServerPlayer player, Identifier stageId) {
        boolean changed = EffectiveStages.add(player, stageId);
        if (changed && EffectiveStages.shouldEmitPerPlayerDelta(player)) {
            PacketDistributor.sendToPlayer(player, new ClientboundStageDeltaPayload(stageId, true));
        }
        return changed;
    }

    public static boolean removeStage(ServerPlayer player, Identifier stageId) {
        boolean changed = EffectiveStages.remove(player, stageId);
        if (changed && EffectiveStages.shouldEmitPerPlayerDelta(player)) {
            PacketDistributor.sendToPlayer(player, new ClientboundStageDeltaPayload(stageId, false));
            InventoryAuditor.auditNow(player);
        }
        return changed;
    }

    public static boolean hasStage(ServerPlayer player, Identifier stageId) {
        return EffectiveStages.has(player, stageId);
    }

    public static Set<Identifier> getStages(ServerPlayer player) {
        return EffectiveStages.of(player);
    }

    public static Optional<StageDefinition> getDefinition(Identifier stageId) {
        return StageManager.get().get(stageId);
    }

    public static void syncAll(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundStagesPayload(getStages(player)));
        syncStageIndicesToPlayer(player);
    }

    /**
     * Sends every stage-locking index (items / fluids / chemicals / recipes) so the recipe viewer on a
     * dedicated-server client can evaluate locks for content defined only on the server (datapack JSON, KubeJS
     * {@code defineStage}, etc.).
     */
    public static void syncStageIndicesToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, currentIndicesPayload());
    }

    /** Broadcast after stage definition rebuild (datapack reload, KubeJS flush, etc.). */
    public static void broadcastStageIndices() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        var payload = currentIndicesPayload();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static ClientboundStageIndicesPayload currentIndicesPayload() {
        StageManager mgr = StageManager.get();
        return new ClientboundStageIndicesPayload(
                mgr.itemStagesIndexView(),
                mgr.fluidStagesIndexView(),
                mgr.chemicalStagesIndexView(),
                mgr.recipeStagesIndexView()
        );
    }
}
