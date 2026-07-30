package com.oreyield.event;

import com.oreyield.OreYieldMod;
import com.oreyield.config.OreConfig;
import com.oreyield.config.OreEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = OreYieldMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DebugOreInfoHandler {
    private static final KeyMapping DEBUG_KEY = new KeyMapping(
            "key.ore_yield.debug_info",
            0x12D,
            "key.categories.ore_yield"
    );
    private static final TagKey<Block> FORGE_ORES = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new ResourceLocation("forge", "ores"));

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

        StringBuilder sb = new StringBuilder();
        sb.append("=== Ore Yield Debug Info ===\n");
        sb.append("Timestamp: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        List<OreEntry> configured = OreConfig.allEntries();
        List<String> configuredIds = configured.stream().map(OreEntry::id).toList();

        List<OreBlockInfo> registryOres = scanRegistryForOres();
        List<OreBlockInfo> moddedOres = registryOres.stream()
                .filter(o -> !o.id.getNamespace().equals("minecraft"))
                .sorted(Comparator.comparing(o -> o.id.toString()))
                .toList();

        sb.append("=== BLOCK REGISTRY SCAN ===\n");
        sb.append("Total ore blocks found: ").append(registryOres.size())
                .append(" (").append(moddedOres.size()).append(" modded)\n\n");

        int configuredCount = 0;
        int notConfiguredCount = 0;

        for (OreBlockInfo ore : moddedOres) {
            boolean inConfig = configuredIds.contains(ore.id.toString());
            boolean inConfigTag = false;
            for (String cid : configuredIds) {
                if (cid.startsWith("#") && ore.block.builtInRegistryHolder().is(
                        TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                                ResourceLocation.tryParse(cid.substring(1))))) {
                    inConfigTag = true;
                    break;
                }
            }
            String status = inConfig ? "[CONFIGURED]" : inConfigTag ? "[IN TAG]" : "[NOT CONFIGURED]";

            if (inConfig || inConfigTag) configuredCount++;
            else notConfiguredCount++;

            String tags = ore.tags.isEmpty() ? "none" : String.join(", ", ore.tags);
            String line = status + " " + ore.id
                    + "  tags=[" + tags + "]"
                    + "  drops=" + ore.drops;

            sb.append(line).append("\n");
        }

        sb.append("\n=== SUMMARY ===\n");
        sb.append("Configured modded ores: ").append(configuredCount).append("\n");
        sb.append("NOT configured (missing from config): ").append(notConfiguredCount).append("\n");

        sb.append("\n=== ORE YIELD CONFIG ENTRIES ===\n");
        for (OreEntry entry : configured) {
            String fortune = switch (entry.fortuneType()) {
                case ORE -> "ORE (multiplicative)";
                case REDSTONE -> "REDSTONE (additive)";
                case NONE -> "NONE";
            };
            sb.append(entry.id())
                    .append("  item=").append(entry.resultItem())
                    .append("  count=[").append(entry.minCount()).append("-").append(entry.maxCount()).append("]")
                    .append("  chance=").append(String.format("%.1f%%", entry.chance() * 100))
                    .append("  Y[").append(entry.minY()).append("..").append(entry.maxY()).append("]")
                    .append("  fortune=").append(fortune)
                    .append("  xp=").append(entry.xpMin()).append("-").append(entry.xpMax())
                    .append("  hosts=").append(String.join(", ", entry.hosts()))
                    .append("\n");
        }

        sb.append("\n=== WORLDGEN / BIOME MODIFIERS ===\n");
        appendBiomeModifierInfo(sb);

        mc.player.sendSystemMessage(Component.literal("Ore Yield: scanning " + moddedOres.size() + " modded ores...").withStyle(ChatFormatting.GOLD));
        mc.player.sendSystemMessage(Component.literal("Configured: " + configuredCount + " | Missing: " + notConfiguredCount).withStyle(notConfiguredCount > 0 ? ChatFormatting.RED : ChatFormatting.GREEN));

        String output = sb.toString();
        if (output.length() > MAX_OUTPUT_BYTES) {
            output = output.substring(0, MAX_OUTPUT_BYTES) + "\n... [truncated, output exceeded 512 KB]\n";
            mc.player.sendSystemMessage(Component.literal("Debug output truncated (exceeded 512 KB limit)").withStyle(ChatFormatting.RED));
        }
        mc.player.sendSystemMessage(Component.literal("Full info saved to ore_yield_debug.txt").withStyle(ChatFormatting.YELLOW));

        Path debugFile = mc.gameDirectory.toPath().resolve("ore_yield_debug.txt");
        try {
            Files.writeString(debugFile, output);
        } catch (IOException e) {
            mc.player.sendSystemMessage(Component.literal("Failed to write debug file: " + e.getMessage()).withStyle(ChatFormatting.RED));
        }
    }

    private static List<OreBlockInfo> scanRegistryForOres() {
        List<OreBlockInfo> result = new ArrayList<>();
        BuiltInRegistries.BLOCK.forEach(block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) return;

            boolean isOre = id.getPath().endsWith("_ore")
                    || block.builtInRegistryHolder().is(FORGE_ORES);

            if (!isOre) return;

            List<String> tags = new ArrayList<>();
            for (TagKey<net.minecraft.world.level.block.Block> tag : getBlockTags(block)) {
                tags.add(tag.location().toString());
            }

            String drops = describeDrops(id);

            result.add(new OreBlockInfo(id, block, tags, drops));
        });
        return result;
    }

    private static List<TagKey<net.minecraft.world.level.block.Block>> getBlockTags(Block block) {
        List<TagKey<net.minecraft.world.level.block.Block>> tags = new ArrayList<>();
        block.builtInRegistryHolder().tags().forEach(tags::add);
        return tags;
    }

    private static String describeDrops(ResourceLocation blockId) {
        ResourceLocation rawId = new ResourceLocation(blockId.getNamespace(), "raw_" + path(blockId));
        if (BuiltInRegistries.ITEM.containsKey(rawId)) return rawId.toString();

        ResourceLocation gemId = new ResourceLocation(blockId.getNamespace(), path(blockId).replace("_ore", ""));
        if (BuiltInRegistries.ITEM.containsKey(gemId)) return gemId.toString();

        ResourceLocation nuggetId = new ResourceLocation(blockId.getNamespace(), path(blockId).replace("_ore", "") + "_nugget");
        if (BuiltInRegistries.ITEM.containsKey(nuggetId)) return nuggetId.toString();

        return blockId.toString() + " (drops itself)";
    }

    private static String path(ResourceLocation id) {
        return id.getPath();
    }

    private static void appendBiomeModifierInfo(StringBuilder sb) {
        sb.append("Biome modifiers control vanilla ore removal (when enabled in config).\n");
        sb.append("To see ore Y-ranges in-game, use F3 debug screen while mining,\n");
        sb.append("or run /locate structure to find ore veins.\n");
        sb.append("For detailed worldgen data, run: gradle runData and check src/generated/resources/.\n");
    }

    private record OreBlockInfo(ResourceLocation id, Block block, List<String> tags, String drops) {}
}
