package com.gabinx.chapters.network;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.RecipeViewerCompat;
import com.gabinx.chapters.stage.ClientStageCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundStageDeltaPayload(Identifier stage, boolean added) implements CustomPacketPayload {
    public static final Type<ClientboundStageDeltaPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Chapters.MOD_ID, "stage_delta"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStageDeltaPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> payload.write(buffer),
                    ClientboundStageDeltaPayload::new
            );

    private ClientboundStageDeltaPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readIdentifier(), buffer.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeIdentifier(stage);
        buffer.writeBoolean(added);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundStageDeltaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.added) {
                ClientStageCache.add(payload.stage);
            } else {
                ClientStageCache.remove(payload.stage);
            }
            RecipeViewerCompat.refresh();
        });
    }
}
