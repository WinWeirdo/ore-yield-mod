package com.oreyield.neoforge.event;

import com.oreyield.OreYieldMod;
import com.oreyield.config.OreYieldConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

//? if eventbus_bus_attribute {
@EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = OreYieldMod.MOD_ID, value = Dist.CLIENT)
//?}
public final class TitleScreenConfigButton {
    private TitleScreenConfigButton() {}

    //? if eventbus_bus_attribute {
    @EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    //?} else {
    @EventBusSubscriber(modid = OreYieldMod.MOD_ID, value = Dist.CLIENT)
    //?}
    public static class ClientSetup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                    () -> (modContainer, screen) -> new OreYieldConfigScreen(screen));
        }
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !mc.isRunning()) return;
        event.addListener(Button.builder(
                Component.literal("Ore Yield Config"),
                //? if minecraft_set_screen_and_show {
                btn -> Minecraft.getInstance().setScreenAndShow(new OreYieldConfigScreen(event.getScreen()))
                //?} else {
                btn -> Minecraft.getInstance().setScreen(new OreYieldConfigScreen(event.getScreen()))
                //?}
        ).bounds(2, 2, 100, 20).build());
    }
}
