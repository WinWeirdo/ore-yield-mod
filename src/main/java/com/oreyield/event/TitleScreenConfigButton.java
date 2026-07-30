package com.oreyield.event;

import com.oreyield.OreYieldMod;
import com.oreyield.config.OreYieldConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TitleScreenConfigButton {
    private TitleScreenConfigButton() {}

    @Mod.EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ModLoadingContext.get().registerExtensionPoint(
                    net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                            parent -> new OreYieldConfigScreen(parent)
                    )
            );
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !mc.isRunning()) return;
        event.addListener(Button.builder(
                Component.literal("Ore Yield Config"),
                btn -> Minecraft.getInstance().setScreen(new OreYieldConfigScreen(event.getScreen()))
        ).bounds(2, 2, 100, 20).build());
    }
}
