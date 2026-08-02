package com.gabinx.chapters.compat.ftb;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.api.ChaptersAPI;
import dev.ftb.mods.ftblibrary.integration.stages.StageProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * FTB Library's stage SPI implementation backed by Chapters.
 * <p>
 * Registered via {@code StageHelper.INSTANCE.setProviderImpl(...)} so that
 * FTB Quests' built-in {@code Stage Reward} and {@code Stage Task} both
 * route through Chapters automatically — making "set a chapter as the
 * reward of a quest" a zero-config feature.
 */
public final class ChaptersStageProvider implements StageProvider {
    @Override
    public boolean has(Player player, String stage) {
        Identifier id = Identifier.tryParse(stage);
        if (id == null) {
            return false;
        }
        if (player instanceof ServerPlayer sp) {
            return ChaptersAPI.hasStage(sp, id);
        }
        return false;
    }

    @Override
    public void add(ServerPlayer player, String stage) {
        Identifier id = Identifier.tryParse(stage);
        if (id == null) {
            Chapters.LOGGER.warn("FTB stage add: invalid id '{}'", stage);
            return;
        }
        ChaptersAPI.addStage(player, id);
    }

    @Override
    public void remove(ServerPlayer player, String stage) {
        Identifier id = Identifier.tryParse(stage);
        if (id == null) {
            Chapters.LOGGER.warn("FTB stage remove: invalid id '{}'", stage);
            return;
        }
        ChaptersAPI.removeStage(player, id);
    }

    @Override
    public void sync(ServerPlayer player) {
        ChaptersAPI.syncAll(player);
    }

    @Override
    public String getName() {
        return "Chapters";
    }
}
