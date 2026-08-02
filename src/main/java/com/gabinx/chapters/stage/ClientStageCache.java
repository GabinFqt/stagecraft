package com.gabinx.chapters.stage;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ClientStageCache {
    private static final Set<Identifier> STAGES = new LinkedHashSet<>();

    private ClientStageCache() {
    }

    public static synchronized void set(Set<Identifier> stages) {
        STAGES.clear();
        STAGES.addAll(stages);
    }

    public static synchronized void add(Identifier stage) {
        STAGES.add(stage);
    }

    public static synchronized void remove(Identifier stage) {
        STAGES.remove(stage);
    }

    public static synchronized Set<Identifier> snapshot() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(STAGES));
    }
}
