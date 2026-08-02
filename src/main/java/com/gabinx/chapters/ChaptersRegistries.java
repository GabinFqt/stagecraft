package com.gabinx.chapters;

import com.gabinx.chapters.network.ClientboundStageDeltaPayload;
import com.gabinx.chapters.network.ClientboundStageIndicesPayload;
import com.gabinx.chapters.network.ClientboundStagesPayload;
import com.gabinx.chapters.stage.PlayerStages;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ChaptersRegistries {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Chapters.MOD_ID);

    public static final Supplier<AttachmentType<PlayerStages>> PLAYER_STAGES = ATTACHMENTS.register(
            "player_stages",
            () -> AttachmentType.builder(PlayerStages::empty)
                    .serialize(PlayerStages.MAP_CODEC)
                    .copyOnDeath()
                    .build()
    );

    private ChaptersRegistries() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(ChaptersRegistries::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToClient(ClientboundStagesPayload.TYPE, ClientboundStagesPayload.STREAM_CODEC, ClientboundStagesPayload::handle);
        registrar.playToClient(ClientboundStageDeltaPayload.TYPE, ClientboundStageDeltaPayload.STREAM_CODEC, ClientboundStageDeltaPayload::handle);
        registrar.playToClient(
                ClientboundStageIndicesPayload.TYPE,
                ClientboundStageIndicesPayload.STREAM_CODEC,
                ClientboundStageIndicesPayload::handle
        );
    }
}
