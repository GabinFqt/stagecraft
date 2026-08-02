package com.gabinx.chapters.compat.mekanism;

import com.gabinx.chapters.stage.StageDefinition;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the chemical → defining stages map when Mekanism is loaded.
 */
@SuppressWarnings("removal")
public final class MekanismChemicalIndex {
    private MekanismChemicalIndex() {
    }

    public static Map<Identifier, Set<Identifier>> buildIndex(List<StageDefinition> mergedDefinitions) {
        Map<Identifier, Set<Identifier>> chemicalMap = new HashMap<>();
        var registry = MekanismAPI.CHEMICAL_REGISTRY;

        for (StageDefinition def : mergedDefinitions) {
            for (Identifier chemicalId : def.chemicals()) {
                chemicalMap.computeIfAbsent(chemicalId, k -> new LinkedHashSet<>()).add(def.id());
            }

            for (Identifier tagId : def.chemicalTags()) {
                TagKey<Chemical> tag = TagKey.create(registry.key(), tagId);
                registry.getTag(tag).ifPresent(holders -> {
                    for (Holder<Chemical> holder : holders) {
                        Chemical chemical = holder.value();
                        Identifier key = registry.getKey(chemical);
                        if (key != null && !MekanismAPI.EMPTY_CHEMICAL_NAME.equals(key)) {
                            chemicalMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(def.id());
                        }
                    }
                });
            }

            for (String ns : def.chemicalNamespaces()) {
                for (Chemical chemical : registry) {
                    if (chemical == null || MekanismAPI.EMPTY_CHEMICAL_NAME.equals(registry.getKey(chemical))) {
                        continue;
                    }
                    Identifier key = registry.getKey(chemical);
                    if (key != null && ns.equals(key.getNamespace())) {
                        chemicalMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(def.id());
                    }
                }
            }
        }

        return chemicalMap;
    }
}
