package com.oreyield.event;

import com.oreyield.config.OreConfig;
import com.oreyield.config.OreEntry;
import com.oreyield.loot.BadLuckEliminator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds the Alt+F12 debug report text. Pure logic, no client classes — each loader's handler renders it. */
public final class DebugReport {
    private static final TagKey<Block> FORGE_ORES = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, new ResourceLocation("forge", "ores"));

    private DebugReport() {}

    public static String buildReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Ore Yield Debug Info ===\n");
        sb.append("Timestamp: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");

        sb.append("=== BAD LUCK ELIMINATOR ===\n");
        if (BadLuckEliminator.isEnabled()) {
            sb.append("Enabled (multiplier x").append(OreConfig.badLuckMultiplier()).append(")\n");
        } else {
            sb.append("Disabled\n");
        }
        sb.append("Guarantee window per ore (eligible blocks without a drop before a 100% drop):\n");
        for (OreEntry entry : OreConfig.allEntries()) {
            int window = BadLuckEliminator.window(entry);
            if (window >= Integer.MAX_VALUE) continue;
            sb.append("  ").append(entry.id())
                    .append("  chance=").append(String.format("%.1f%%", entry.chance() * 100))
                    .append("  window=").append(window).append("\n");
        }
        sb.append("\n");

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

        return sb.toString();
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
        sb.append("For detailed worldgen data, run: gradlew :forge:runData and check forge/src/generated/resources/.\n");
    }

    private record OreBlockInfo(ResourceLocation id, Block block, List<String> tags, String drops) {}
}
