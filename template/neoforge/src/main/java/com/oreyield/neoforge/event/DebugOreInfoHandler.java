package com.oreyield.neoforge.event;

import com.oreyield.OreYieldMod;
import com.oreyield.event.DebugReport;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//? if eventbus_bus_attribute {
@EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = OreYieldMod.MOD_ID, value = Dist.CLIENT)
//?}
public final class DebugOreInfoHandler {
    private static final KeyMapping DEBUG_KEY = new KeyMapping(
            "key.ore_yield.debug_info",
            0x12D,
            //? if keymapping_category {
            KeyMapping.Category.MISC
            //?} else {
            "key.categories.ore_yield"
            //?}
    );

    private static final int MAX_OUTPUT_BYTES = 524288; // 512 KB limit

    private DebugOreInfoHandler() {}

    //? if eventbus_bus_attribute {
    @EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    //?} else {
    @EventBusSubscriber(modid = OreYieldMod.MOD_ID, value = Dist.CLIENT)
    //?}
    public static class ClientSetup {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(DEBUG_KEY);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!DEBUG_KEY.consumeClick()) return;

        //? if window_handle {
        long windowHandle = Minecraft.getInstance().getWindow().handle();
        //?} else {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        //?}
        boolean altHeld = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        if (!altHeld) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String output = DebugReport.buildReport(mc.getSingleplayerServer() != null
                ? mc.getSingleplayerServer().registryAccess() : null);
        if (output.length() > MAX_OUTPUT_BYTES) {
            output = output.substring(0, MAX_OUTPUT_BYTES) + "\n... [truncated, output exceeded 512 KB]\n";
            //? if player_display_client_message {
            mc.player.displayClientMessage(Component.literal("Debug output truncated (exceeded 512 KB limit)").withStyle(ChatFormatting.RED), false);
            //?} else {
            mc.player.sendSystemMessage(Component.literal("Debug output truncated (exceeded 512 KB limit)").withStyle(ChatFormatting.RED));
            //?}
        }
        //? if player_display_client_message {
        mc.player.displayClientMessage(Component.literal("Ore Yield: debug info saved to ore_yield_debug.txt").withStyle(ChatFormatting.YELLOW), false);
        //?} else {
        mc.player.sendSystemMessage(Component.literal("Ore Yield: debug info saved to ore_yield_debug.txt").withStyle(ChatFormatting.YELLOW));
        //?}

        Path debugFile = mc.gameDirectory.toPath().resolve("ore_yield_debug.txt");
        try {
            Files.writeString(debugFile, output);
        } catch (IOException e) {
            //? if player_display_client_message {
            mc.player.displayClientMessage(Component.literal("Failed to write debug file: " + e.getMessage()).withStyle(ChatFormatting.RED), false);
            //?} else {
            mc.player.sendSystemMessage(Component.literal("Failed to write debug file: " + e.getMessage()).withStyle(ChatFormatting.RED));
            //?}
        }
    }
}
