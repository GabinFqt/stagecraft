package com.gabinx.chapters.compat.ftb;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.TeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.api.neoforge.FTBTeamsEvent;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftbteams.api.property.TeamPropertyCollection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Direct FTB Teams interactions. This class only loads when {@code ftbteams}
 * is on the classpath; callers must guard with {@link FtbCompat#isTeamsLoaded()}.
 * <p>
 * <b>Why we don't use {@code TeamStagesHelper}:</b> {@link TeamProperties#TEAM_STAGES} is declared with a single
 * {@code new HashSet<>()} as its default value. {@code StringSetProperty}'s constructor wraps that instance in a
 * {@code Supplier} that always returns the very same instance, so every team that has not explicitly set
 * {@code TEAM_STAGES} reads back the same shared {@code HashSet}. {@code TeamStagesHelper.updateStages} mutates that
 * set in place before calling {@code setProperty(...)} — meaning a write on team A leaks into the shared default that
 * team B will read for the first time. We avoid the issue by always copying the current value into a fresh
 * {@code HashSet} before mutating, and by detecting reads that hit the shared default via identity comparison.
 */
final class FtbTeamsBridge {
    private FtbTeamsBridge() {
    }

    static boolean hasTeam(ServerPlayer player) {
        return FTBTeamsAPI.api().isManagerLoaded()
                && FTBTeamsAPI.api().getManager().getTeamForPlayer(player).isPresent();
    }

    @Nullable
    static Team teamOf(ServerPlayer player) {
        if (!FTBTeamsAPI.api().isManagerLoaded()) {
            return null;
        }
        return FTBTeamsAPI.api().getManager().getTeamForPlayer(player).orElse(null);
    }

    /**
     * @return the team's stage set as {@link Identifier}s, or {@code null}
     *         when the team manager is not loaded yet (caller should fall back).
     */
    @Nullable
    static Set<Identifier> viewStages(ServerPlayer player) {
        Team team = teamOf(player);
        if (team == null) {
            return null;
        }
        return toIdentifiers(safeReadStages(team));
    }

    /**
     * @return {@code null} when no team manager / no team (caller should fall back),
     *         {@link Boolean#TRUE}/{@link Boolean#FALSE} otherwise.
     */
    @Nullable
    static Boolean hasStage(ServerPlayer player, Identifier stage) {
        Team team = teamOf(player);
        if (team == null) {
            return null;
        }
        return safeReadStages(team).contains(stage.toString());
    }

    /**
     * Snapshot of a team's TEAM_STAGES as mutable strings (empty when unset / shared default).
     */
    static Set<String> readStageStrings(Team team) {
        return new HashSet<>(safeReadStages(team));
    }

    /**
     * Replace the team's TEAM_STAGES with {@code stages} (copied) and notify listeners.
     */
    static void writeStageStrings(Team team, Set<String> stages) {
        if (team == null || team.isClientTeam()) {
            return;
        }
        commitStages(team, new HashSet<>(stages));
    }

    static boolean addStage(ServerPlayer player, Identifier stage) {
        Team team = teamOf(player);
        if (team == null || team.isClientTeam()) {
            return false;
        }
        Set<String> next = isolatedCopy(team);
        if (!next.add(stage.toString())) {
            return false;
        }
        commitStages(team, next);
        return true;
    }

    static boolean removeStage(ServerPlayer player, Identifier stage) {
        Team team = teamOf(player);
        if (team == null || team.isClientTeam()) {
            return false;
        }
        Set<String> next = isolatedCopy(team);
        if (!next.remove(stage.toString())) {
            return false;
        }
        commitStages(team, next);
        return true;
    }

    /**
     * Read TEAM_STAGES, treating the shared FTB default instance as empty. Any value the team has explicitly set
     * (including ones we wrote ourselves) is returned as-is.
     */
    private static Set<String> safeReadStages(Team team) {
        Set<String> raw = team.getProperty(TeamProperties.TEAM_STAGES);
        return raw == TeamProperties.TEAM_STAGES.getDefaultValue() ? Set.of() : raw;
    }

    /**
     * Build a fresh, mutable {@code HashSet} from the team's current stages without touching the shared default.
     */
    private static Set<String> isolatedCopy(Team team) {
        return new HashSet<>(safeReadStages(team));
    }

    /**
     * Commit a freshly built stage set on the team and fire the same listeners {@code TeamStagesHelper.updateStages}
     * would, so {@link FtbTeamsListeners} and any third-party listeners react identically.
     */
    private static void commitStages(Team team, Set<String> next) {
        TeamPropertyCollection old = team.getProperties().copy();
        team.setProperty(TeamProperties.TEAM_STAGES, next);
        NeoForge.EVENT_BUS.post(new FTBTeamsEvent.TeamPropertiesChanged(
                new TeamPropertiesChangedEvent.Data(team, old, false)
        ));
        team.syncOnePropertyToTeam(TeamProperties.TEAM_STAGES, next);
    }

    private static Set<Identifier> toIdentifiers(Iterable<String> raw) {
        Set<Identifier> out = new LinkedHashSet<>();
        for (String s : raw) {
            Identifier rl = Identifier.tryParse(s);
            if (rl != null) {
                out.add(rl);
            }
        }
        return out;
    }
}
