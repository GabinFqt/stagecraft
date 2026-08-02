package com.gabinx.chapters.compat.ftb;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.ChaptersRegistries;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.event.InventoryAuditor;
import com.gabinx.chapters.stage.PlayerStages;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.api.neoforge.FTBTeamsEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Wires Chapters reactions to FTB Teams events. Only loaded when both
 * {@code ftblibrary} and {@code ftbteams} are present.
 */
final class FtbTeamsListeners {
    private FtbTeamsListeners() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(FtbTeamsListeners::onPropertiesChanged);
        NeoForge.EVENT_BUS.addListener(FtbTeamsListeners::onPlayerChangedTeam);
        NeoForge.EVENT_BUS.addListener(FtbTeamsListeners::onJoinedParty);
        NeoForge.EVENT_BUS.addListener(FtbTeamsListeners::onLeftParty);
        NeoForge.EVENT_BUS.addListener(FtbTeamsListeners::onPlayerLoggedIn);
        Chapters.LOGGER.info("Chapters: FTB Teams listeners registered");
    }

    private static void onPropertiesChanged(FTBTeamsEvent.TeamPropertiesChanged event) {
        TeamPropertiesChangedEvent.Data data = event.getEventData();
        Team team = data.team();
        if (team == null || team.isClientTeam() || data.isClient()) {
            return;
        }
        if (!data.hasPropertyChanged(TeamProperties.TEAM_STAGES)) {
            return;
        }
        propagateToTeamMembers(team);
    }

    private static void onPlayerChangedTeam(FTBTeamsEvent.PlayerChangedTeam event) {
        ServerPlayer player = event.getEventData().player();
        if (player == null) {
            return;
        }
        ChaptersAPI.syncAll(player);
        InventoryAuditor.auditNow(player);
    }

    /**
     * Party join: union previous personal stages + current party stages into the party store,
     * so every online member sees the merged set.
     */
    private static void onJoinedParty(FTBTeamsEvent.PlayerJoinedPartyTeam event) {
        var data = event.getEventData();
        Team party = data.team();
        Team previous = data.previousTeam();
        ServerPlayer player = data.player();
        if (party == null || party.isClientTeam()) {
            return;
        }

        Set<String> merged = new HashSet<>();
        merged.addAll(FtbTeamsBridge.readStageStrings(party));
        if (previous != null) {
            merged.addAll(FtbTeamsBridge.readStageStrings(previous));
        }
        if (player != null) {
            for (Identifier id : player.getData(ChaptersRegistries.PLAYER_STAGES.get()).view()) {
                merged.add(id.toString());
            }
        }

        FtbTeamsBridge.writeStageStrings(party, merged);
        Chapters.LOGGER.info(
                "Chapters: merged {} stage(s) into party {} after {} joined",
                merged.size(),
                party.getShortName(),
                player != null ? player.getGameProfile().name() : "?"
        );
        // writeStageStrings already fires TeamPropertiesChanged → propagateToTeamMembers
    }

    /**
     * Party leave: copy the party's stages onto the player's new personal team so they keep
     * everything they had while in the party, but are no longer synced with the party afterward.
     */
    private static void onLeftParty(FTBTeamsEvent.PlayerLeftPartyTeam event) {
        var data = event.getEventData();
        Team party = data.team();
        Team personal = data.playerTeam();
        ServerPlayer player = data.player();

        Set<String> snapshot = party != null ? FtbTeamsBridge.readStageStrings(party) : Set.of();
        if (personal != null && !personal.isClientTeam()) {
            FtbTeamsBridge.writeStageStrings(personal, snapshot);
            Chapters.LOGGER.info(
                    "Chapters: copied {} party stage(s) onto personal team {} after leave",
                    snapshot.size(),
                    personal.getShortName()
            );
        }

        if (player != null) {
            ChaptersAPI.syncAll(player);
            InventoryAuditor.auditNow(player);
        }
    }

    /**
     * One-time migration: when a player logs in for the first time after FTB Teams
     * is installed, copy any pre-existing per-player attachment stages into their
     * personal team's {@code TEAM_STAGES}. Skipped for party teams to avoid leaking
     * personal stages into a shared party.
     */
    private static void onPlayerLoggedIn(FTBTeamsEvent.TeamPlayerLoggedIn event) {
        var data = event.getEventData();
        ServerPlayer player = data.player();
        if (player == null) {
            return;
        }

        Team team = data.team();
        if (team == null || team.isClientTeam() || team.isPartyTeam()) {
            ChaptersAPI.syncAll(player);
            return;
        }

        PlayerStages legacy = player.getData(ChaptersRegistries.PLAYER_STAGES.get());
        if (legacy.view().isEmpty()) {
            ChaptersAPI.syncAll(player);
            return;
        }

        int added = 0;
        for (Identifier rl : legacy.view()) {
            if (FtbTeamsBridge.addStage(player, rl)) {
                added++;
            }
        }
        if (added > 0) {
            Chapters.LOGGER.info(
                    "Chapters: migrated {} legacy stage(s) from player {} attachment into personal team {}",
                    added,
                    player.getGameProfile().name(),
                    team.getShortName()
            );
        }
        ChaptersAPI.syncAll(player);
    }

    private static void propagateToTeamMembers(Team team) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null && belongsToTeam(member, team)) {
                ChaptersAPI.syncAll(member);
                InventoryAuditor.auditNow(member);
            }
        }
    }

    private static boolean belongsToTeam(ServerPlayer player, Team team) {
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player)
                .map(t -> t.getId().equals(team.getId()))
                .orElse(false);
    }
}
