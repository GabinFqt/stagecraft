package com.gabinx.chapters;

import com.mojang.logging.LogUtils;
import com.gabinx.chapters.command.ChaptersCommand;
import com.gabinx.chapters.compat.RecipeViewerBootstrap;
import com.gabinx.chapters.compat.ftb.FtbCompat;
import com.gabinx.chapters.compat.kubejs.ChaptersKubeJSBridge;
import com.gabinx.chapters.event.CraftingHandler;
import com.gabinx.chapters.event.InventoryAuditor;
import com.gabinx.chapters.event.PickupHandler;
import com.gabinx.chapters.event.PlayerJoinHandler;
import com.gabinx.chapters.stage.StageManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(Chapters.MOD_ID)
public final class Chapters {
    public static final String MOD_ID = "chapters";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final Identifier STAGE_RELOAD_LISTENER_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "stages");

    public Chapters(IEventBus modBus) {
        ChaptersRegistries.register(modBus);

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(PickupHandler::onPickup);
        NeoForge.EVENT_BUS.addListener(CraftingHandler::onCrafted);
        NeoForge.EVENT_BUS.addListener(InventoryAuditor::onTick);
        NeoForge.EVENT_BUS.addListener(ChaptersKubeJSBridge::onServerTickPost);
        NeoForge.EVENT_BUS.addListener(PlayerJoinHandler::onLogin);
        NeoForge.EVENT_BUS.addListener(PlayerJoinHandler::onRespawn);

        FtbCompat.bootstrap();

        RecipeViewerBootstrap.logDetectedRecipeViewers();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ChaptersCommand.register(event.getDispatcher());
    }

    private void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(STAGE_RELOAD_LISTENER_ID, StageManager.get());
    }
}
