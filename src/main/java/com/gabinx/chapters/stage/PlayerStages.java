package com.gabinx.chapters.stage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PlayerStages {
    public static final Codec<PlayerStages> CODEC = Identifier.CODEC.listOf()
            .xmap(PlayerStages::new, stages -> List.copyOf(stages.stages));

    /** Attachment persistence expects a {@link MapCodec}. */
    public static final MapCodec<PlayerStages> MAP_CODEC = CODEC.fieldOf("stages");

    private final Set<Identifier> stages;

    public PlayerStages(Collection<Identifier> stages) {
        this.stages = new LinkedHashSet<>(stages);
    }

    public static PlayerStages empty() {
        return new PlayerStages(List.of());
    }

    public Set<Identifier> view() {
        return Collections.unmodifiableSet(stages);
    }

    public boolean add(Identifier stage) {
        return stages.add(stage);
    }

    public boolean remove(Identifier stage) {
        return stages.remove(stage);
    }

    public boolean has(Identifier stage) {
        return stages.contains(stage);
    }
}
