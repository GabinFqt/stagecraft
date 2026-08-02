package com.gabinx.chapters.network;

import com.gabinx.chapters.Chapters;
import com.gabinx.chapters.compat.RecipeViewerCompat;
import com.gabinx.chapters.stage.ClientStageCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashSet;
import java.util.Set;

public record ClientboundStagesPayload(Set<Identifier> stages) implements CustomPacketPayload {
    public static final Type<ClientboundStagesPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Chapters.MOD_ID, "stages"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundStagesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(LinkedHashSet::new, Identifier.STREAM_CODEC),
                    ClientboundStagesPayload::stages,
                    ClientboundStagesPayload::new
            );

    public ClientboundStagesPayload {
        stages = Set.copyOf(stages);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundStagesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientStageCache.set(payload.stages);
            RecipeViewerCompat.refresh();
        });
    }
}
