package com.gabinx.chapters.stage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.api.ChaptersAPI;
import com.gabinx.chapters.event.InventoryAuditor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class StageManager extends SimplePreparableReloadListener<Map<Identifier, JsonObject>> {
    private static final FileToIdConverter LISTER = FileToIdConverter.json("chapters/stages");
    private static final StageManager INSTANCE = new StageManager();

    private final Map<Identifier, StageDefinition> datapackDefinitions = new LinkedHashMap<>();
    private final Map<Identifier, StageDefinition> runtimeDefinitions = new LinkedHashMap<>();

    /** Merged datapack + runtime definitions (immutable snapshot). */
    private List<StageDefinition> mergedDefinitions = List.of();

    /**
     * Item registry key → stage ids that gate this item (expanded from definitions). Used for fast lock checks and
     * recipe viewers.
     */
    private Map<Identifier, Set<Identifier>> itemStagesIndex = Map.of();

    /** Fluid kind registry key → stage ids that gate this fluid. */
    private Map<Identifier, Set<Identifier>> fluidStagesIndex = Map.of();

    /** Mekanism chemical registry key → stage ids that gate this chemical (empty when Mekanism is absent). */
    private Map<Identifier, Set<Identifier>> chemicalStagesIndex = Map.of();

    /** Recipe holder id → stage ids that gate this recipe. */
    private Map<Identifier, Set<Identifier>> recipeStagesIndex = Map.of();

    private StageManager() {
    }

    public static StageManager get() {
        return INSTANCE;
    }

    @Override
    protected Map<Identifier, JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonObject> next = new LinkedHashMap<>();
        for (var entry : LISTER.listMatchingResources(resourceManager).entrySet()) {
            Identifier fileId = entry.getKey();
            Identifier id = LISTER.fileToId(fileId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    Chapters.LOGGER.warn("Ignoring non-object stage definition {}", id);
                    continue;
                }
                next.put(id, element.getAsJsonObject());
            } catch (Exception e) {
                Chapters.LOGGER.error("Failed to read stage definition {} from {}", id, fileId, e);
            }
        }
        return next;
    }

    @Override
    protected void apply(Map<Identifier, JsonObject> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, StageDefinition> next = new LinkedHashMap<>();
        objects.forEach((id, json) -> next.put(id, StageDefinition.fromJson(id, json)));

        synchronized (this) {
            datapackDefinitions.clear();
            datapackDefinitions.putAll(next);
            rebuildMergedAndIndicesLocked();
        }
        Chapters.LOGGER.info("Loaded {} datapack stage definitions", next.size());
        auditLoadedPlayersAfterReload();
    }

    public synchronized void setRuntimeDefinitions(Collection<StageDefinition> runtime) {
        runtimeDefinitions.clear();
        for (StageDefinition definition : runtime) {
            runtimeDefinitions.put(definition.id(), definition);
        }
        rebuildMergedAndIndicesLocked();
        auditLoadedPlayersAfterReload();
    }

    private void rebuildMergedAndIndicesLocked() {
        Map<Identifier, StageDefinition> merged = new LinkedHashMap<>(datapackDefinitions);
        merged.putAll(runtimeDefinitions);
        mergedDefinitions = List.copyOf(merged.values());

        Map<Identifier, Set<Identifier>> itemMap = new HashMap<>();
        Map<Identifier, Set<Identifier>> fluidMap = new HashMap<>();
        Map<Identifier, Set<Identifier>> recipeMap = new HashMap<>();

        for (StageDefinition def : mergedDefinitions) {
            for (Identifier itemId : def.items()) {
                itemMap.computeIfAbsent(itemId, k -> new LinkedHashSet<>()).add(def.id());
            }

            for (TagKey<Item> tag : def.tags()) {
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    Identifier key = BuiltInRegistries.ITEM.getKey(holder.value());
                    if (key != null) {
                        itemMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(def.id());
                    }
                }
            }

            for (String ns : def.namespaces()) {
                for (Item item : BuiltInRegistries.ITEM) {
                    Identifier key = BuiltInRegistries.ITEM.getKey(item);
                    if (key != null && ns.equals(key.getNamespace())) {
                        itemMap.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(def.id());
                    }
                }
            }

            for (Identifier fluidId : def.fluids()) {
                fluidMap.computeIfAbsent(fluidId, k -> new LinkedHashSet<>()).add(def.id());
            }

            for (TagKey<Fluid> tag : def.fluidTags()) {
                for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
                    Fluid fluid = holder.value();
                    Identifier kind = StageDefinition.fluidKindRegistryKey(fluid);
                    if (kind != null) {
                        fluidMap.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(def.id());
                    }
                }
            }

            for (String ns : def.fluidNamespaces()) {
                for (Fluid fluid : BuiltInRegistries.FLUID) {
                    if (fluid == null || fluid == Fluids.EMPTY) {
                        continue;
                    }
                    Identifier kind = StageDefinition.fluidKindRegistryKey(fluid);
                    if (kind != null && ns.equals(kind.getNamespace())) {
                        fluidMap.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(def.id());
                    }
                }
            }

            for (Identifier recipeId : def.recipes()) {
                recipeMap.computeIfAbsent(recipeId, k -> new LinkedHashSet<>()).add(def.id());
            }
        }

        itemStagesIndex = Map.copyOf(itemMap);
        fluidStagesIndex = Map.copyOf(fluidMap);
        chemicalStagesIndex = rebuildChemicalStagesIndex(mergedDefinitions);
        recipeStagesIndex = Map.copyOf(recipeMap);
        ChaptersAPI.broadcastStageIndices();
    }

    @SuppressWarnings("unchecked")
    private static Map<Identifier, Set<Identifier>> rebuildChemicalStagesIndex(List<StageDefinition> defs) {
        if (!ModList.get().isLoaded("mekanism")) {
            return Map.of();
        }
        try {
            Class<?> indexClass = Class.forName("com.gabinx.chapters.compat.mekanism.MekanismChemicalIndex");
            Object raw = indexClass.getMethod("buildIndex", List.class).invoke(null, defs);
            return Map.copyOf((Map<Identifier, Set<Identifier>>) raw);
        } catch (ReflectiveOperationException e) {
            Chapters.LOGGER.error("Failed to build Mekanism chemical stage index", e);
            return Map.of();
        }
    }

    private static void auditLoadedPlayersAfterReload() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            InventoryAuditor.auditNow(player);
        }
    }

    /**
     * Snapshot of item registry key → defining stage ids (for recipe viewers). Keys are only items referenced by at
     * least one stage rule.
     */
    public synchronized Map<Identifier, Set<Identifier>> itemStagesIndexView() {
        return itemStagesIndex;
    }

    /**
     * Snapshot of fluid kind key → defining stage ids (for recipe viewers).
     */
    public synchronized Map<Identifier, Set<Identifier>> fluidStagesIndexView() {
        return fluidStagesIndex;
    }

    /**
     * Mekanism chemical id → defining stage ids (for recipe viewers). Empty when Mekanism is not installed.
     */
    public synchronized Map<Identifier, Set<Identifier>> chemicalStagesIndexView() {
        return chemicalStagesIndex;
    }

    /**
     * Snapshot of recipe id → defining stage ids (for recipe viewers and lock checks).
     */
    public synchronized Map<Identifier, Set<Identifier>> recipeStagesIndexView() {
        return recipeStagesIndex;
    }

    public synchronized Map<Identifier, StageDefinition> allDefinitions() {
        Map<Identifier, StageDefinition> merged = new LinkedHashMap<>(datapackDefinitions);
        merged.putAll(runtimeDefinitions);
        return merged;
    }

    public synchronized Optional<StageDefinition> get(Identifier stageId) {
        StageDefinition runtime = runtimeDefinitions.get(stageId);
        if (runtime != null) {
            return Optional.of(runtime);
        }
        return Optional.ofNullable(datapackDefinitions.get(stageId));
    }

    public synchronized Set<Identifier> stageIds() {
        Set<Identifier> ids = new LinkedHashSet<>(datapackDefinitions.keySet());
        ids.addAll(runtimeDefinitions.keySet());
        return ids;
    }

    /**
     * Whether the stack is locked for this player: every defining stage must be absent for “locked”.
     */
    public synchronized boolean isItemLocked(PlayerStages stages, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return false;
        }
        Set<Identifier> defining = itemStagesIndex.get(key);
        if (defining == null || defining.isEmpty()) {
            return false;
        }
        for (Identifier stageId : defining) {
            if (stages.has(stageId)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean isFluidLocked(PlayerStages stages, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier kind = StageDefinition.fluidKindRegistryKey(stack.getFluid());
        if (kind == null) {
            return false;
        }
        Set<Identifier> defining = fluidStagesIndex.get(kind);
        if (defining == null || defining.isEmpty()) {
            return false;
        }
        for (Identifier stageId : defining) {
            if (stages.has(stageId)) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean isChemicalLocked(PlayerStages stages, Identifier chemicalRegistryKey) {
        if (chemicalRegistryKey == null) {
            return false;
        }
        Set<Identifier> defining = chemicalStagesIndex.get(chemicalRegistryKey);
        if (defining == null || defining.isEmpty()) {
            return false;
        }
        for (Identifier stageId : defining) {
            if (stages.has(stageId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Locked when any stage lists this recipe id and the player has none of those stages.
     */
    public synchronized boolean isRecipeLocked(PlayerStages stages, Identifier recipeHolderId) {
        if (recipeHolderId == null) {
            return false;
        }
        Set<Identifier> defining = recipeStagesIndex.get(recipeHolderId);
        if (defining == null || defining.isEmpty()) {
            return false;
        }
        for (Identifier stageId : defining) {
            if (stages.has(stageId)) {
                return false;
            }
        }
        return true;
    }
}
