package com.gabinx.chapters.network;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.RecipeViewerCompat;
import com.gabinx.chapters.stage.ClientStageIndices;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Server-to-client payload carrying every stage-locking index (items, fluids, chemicals, recipes) in a single message.
 * Mirrors {@link com.gabinx.chapters.stage.StageManager} indices into {@link ClientStageIndices} so that
 * {@link com.gabinx.chapters.compat.RecipeViewerCompat} can compute locked sets on the logical client.
 */
public record ClientboundStageIndicesPayload(
        Map<Identifier, Set<Identifier>> itemStages,
        Map<Identifier, Set<Identifier>> fluidStages,
        Map<Identifier, Set<Identifier>> chemicalStages,
        Map<Identifier, Set<Identifier>> recipeStages
) implements CustomPacketPayload {

    public ClientboundStageIndicesPayload {
        itemStages = Map.copyOf(itemStages);
        fluidStages = Map.copyOf(fluidStages);
        chemicalStages = Map.copyOf(chemicalStages);
        recipeStages = Map.copyOf(recipeStages);
    }

    public static final Type<ClientboundStageIndicesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Chapters.MOD_ID, "stage_indices"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStageIndicesPayload> STREAM_CODEC =
            StreamCodec.of(ClientboundStageIndicesPayload::write, ClientboundStageIndicesPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, ClientboundStageIndicesPayload payload) {
        writeMap(buf, payload.itemStages);
        writeMap(buf, payload.fluidStages);
        writeMap(buf, payload.chemicalStages);
        writeMap(buf, payload.recipeStages);
    }

    private static ClientboundStageIndicesPayload read(RegistryFriendlyByteBuf buf) {
        return new ClientboundStageIndicesPayload(readMap(buf), readMap(buf), readMap(buf), readMap(buf));
    }

    private static void writeMap(RegistryFriendlyByteBuf buf, Map<Identifier, Set<Identifier>> map) {
        buf.writeVarInt(map.size());
        for (Map.Entry<Identifier, Set<Identifier>> e : map.entrySet()) {
            Identifier.STREAM_CODEC.encode(buf, e.getKey());
            buf.writeVarInt(e.getValue().size());
            for (Identifier s : e.getValue()) {
                Identifier.STREAM_CODEC.encode(buf, s);
            }
        }
    }

    private static Map<Identifier, Set<Identifier>> readMap(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<Identifier, Set<Identifier>> map = new LinkedHashMap<>(n);
        for (int i = 0; i < n; i++) {
            Identifier key = Identifier.STREAM_CODEC.decode(buf);
            int m = buf.readVarInt();
            LinkedHashSet<Identifier> values = new LinkedHashSet<>(Math.max(m, 1));
            for (int j = 0; j < m; j++) {
                values.add(Identifier.STREAM_CODEC.decode(buf));
            }
            map.put(key, values);
        }
        return map;
    }

    public static void handle(ClientboundStageIndicesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientStageIndices.replace(
                    payload.itemStages,
                    payload.fluidStages,
                    payload.chemicalStages,
                    payload.recipeStages
            );
            RecipeViewerCompat.refresh();
        });
    }
}
