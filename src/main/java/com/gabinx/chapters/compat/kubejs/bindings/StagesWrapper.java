package com.gabinx.chapters.compat.kubejs.bindings;

import com.gabinx.chapters.api.ChaptersAPI;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.stream.Collectors;

public final class StagesWrapper {
    private final ServerPlayer player;

    public StagesWrapper(ServerPlayer player) {
        this.player = player;
    }

    public boolean add(String stage) {
        Identifier id = Identifier.tryParse(stage);
        return id != null && ChaptersAPI.addStage(player, id);
    }

    public boolean remove(String stage) {
        Identifier id = Identifier.tryParse(stage);
        return id != null && ChaptersAPI.removeStage(player, id);
    }

    public boolean has(String stage) {
        Identifier id = Identifier.tryParse(stage);
        return id != null && ChaptersAPI.hasStage(player, id);
    }

    public Set<String> get() {
        return ChaptersAPI.getStages(player).stream()
                .map(Identifier::toString)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
