package com.oreyield.fabric.client;

import com.oreyield.event.DebugReport;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//? if fabric_keymapping_renamed {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
//?}
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DebugOreInfoHandlerFabric implements ClientModInitializer {
    private static final KeyMapping DEBUG_KEY = registerDebugKey();

    private static KeyMapping registerDebugKey() {
        //? if fabric_keymapping_renamed {
        return KeyMappingHelper.registerKeyMapping(
        //?} else {
        return KeyBindingHelper.registerKeyBinding(
        //?}
                new KeyMapping("key.ore_yield.debug_info", GLFW.GLFW_KEY_F12,
                        //? if keymapping_category {
                        KeyMapping.Category.MISC
                        //?} else {
                        "key.categories.ore_yield"
                        //?}
                )
        );
    }

    private static final int MAX_OUTPUT_BYTES = 524288; // 512 KB limit

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (DEBUG_KEY.consumeClick()) {
                //? if window_handle {
                long window = client.getWindow().handle();
                //?} else {
                long window = client.getWindow().getWindow();
                //?}
                boolean altHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                        || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
                if (!altHeld) continue;
                if (client.player == null) continue;

                String output = DebugReport.buildReport(client.getSingleplayerServer() != null
                        ? client.getSingleplayerServer().registryAccess() : null);
                if (output.length() > MAX_OUTPUT_BYTES) {
                    output = output.substring(0, MAX_OUTPUT_BYTES) + "\n... [truncated, output exceeded 512 KB]\n";
                    //? if player_display_client_message {
                    client.player.displayClientMessage(Component.literal("Debug output truncated (exceeded 512 KB limit)").withStyle(ChatFormatting.RED), false);
                    //?} else {
                    client.player.sendSystemMessage(Component.literal("Debug output truncated (exceeded 512 KB limit)").withStyle(ChatFormatting.RED));
                    //?}
                }
                //? if player_display_client_message {
                client.player.displayClientMessage(Component.literal("Ore Yield: debug info saved to ore_yield_debug.txt").withStyle(ChatFormatting.YELLOW), false);
                //?} else {
                client.player.sendSystemMessage(Component.literal("Ore Yield: debug info saved to ore_yield_debug.txt").withStyle(ChatFormatting.YELLOW));
                //?}

                Path debugFile = client.gameDirectory.toPath().resolve("ore_yield_debug.txt");
                try {
                    Files.writeString(debugFile, output);
                } catch (IOException e) {
                    //? if player_display_client_message {
                    client.player.displayClientMessage(Component.literal("Failed to write debug file: " + e.getMessage()).withStyle(ChatFormatting.RED), false);
                    //?} else {
                    client.player.sendSystemMessage(Component.literal("Failed to write debug file: " + e.getMessage()).withStyle(ChatFormatting.RED));
                    //?}
                }
            }
        });
    }
}
