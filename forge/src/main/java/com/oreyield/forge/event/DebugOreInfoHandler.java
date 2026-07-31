package com.oreyield.forge.event;

import com.oreyield.OreYieldMod;
import com.oreyield.event.DebugReport;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DebugOreInfoHandler {
    private static final KeyMapping DEBUG_KEY = new KeyMapping(
            "key.ore_yield.debug_info",
            0x12D,
            "key.categories.ore_yield"
    );

    private static final int MAX_OUTPUT_BYTES = 524288; // 512 KB limit

    private DebugOreInfoHandler() {}

    @Mod.EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetup {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(DEBUG_KEY);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!DEBUG_KEY.consumeClick()) return;

        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean altHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        if (!altHeld) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String output = DebugReport.buildReport();
        if (output.length() > MAX_OUTPUT_BYTES) {
            output = output.substring(0, MAX_OUTPUT_BYTES) + "\n... [truncated, output exceeded 512 KB]\n";
            mc.player.sendSystemMessage(Component.literal("Debug output truncated (exceeded 512 KB limit)").withStyle(ChatFormatting.RED));
        }
        mc.player.sendSystemMessage(Component.literal("Ore Yield: debug info saved to ore_yield_debug.txt").withStyle(ChatFormatting.YELLOW));

        Path debugFile = mc.gameDirectory.toPath().resolve("ore_yield_debug.txt");
        try {
            Files.writeString(debugFile, output);
        } catch (IOException e) {
            mc.player.sendSystemMessage(Component.literal("Failed to write debug file: " + e.getMessage()).withStyle(ChatFormatting.RED));
        }
    }
}
