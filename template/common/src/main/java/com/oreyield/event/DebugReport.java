package com.oreyield.event;

import com.oreyield.config.OreConfig;
import com.oreyield.config.OreEntry;
import com.oreyield.loot.BadLuckEliminator;
import com.oreyield.util.ResourceLocations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds the Alt+F12 debug report text. Pure logic, no client classes — each loader's handler renders it. */
public final class DebugReport {
    //? if tier_item_map {
    private static final TagKey<Block> ORE_TAG = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ResourceLocations.of("c", "ores"));
    private static final String BLOCK_TAG_DIRECTORY = "tags/block";
    private static final String ITEM_TAG_DIRECTORY = "tags/item";
    //?} else {
    private static final TagKey<Block> ORE_TAG = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, ResourceLocations.of("forge", "ores"));
    private static final String BLOCK_TAG_DIRECTORY = "tags/blocks";
    private static final String ITEM_TAG_DIRECTORY = "tags/items";
    //?}

    private static final List<String> HOST_PROBE_BLOCKS = List.of(
            "minecraft:stone", "minecraft:deepslate", "minecraft:tuff", "minecraft:andesite",
            "minecraft:netherrack", "minecraft:blackstone", "minecraft:basalt", "minecraft:end_stone");

    private DebugReport() {}

    public static String buildReport() {
        return buildReport(null);
    }

    /** @param registryAccess server registry access (singleplayer); may be null, then holder-based fallback is used */
    public static String buildReport(net.minecraft.core.RegistryAccess registryAccess) {
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
            boolean inConfig = false;
            boolean inHosts = false;
            for (OreEntry entry : configured) {
                String entryId = entry.id();
                if (ore.id.toString().equals(entryId) || ore.id.toString().equals(entryId + "_ore")) {
                    inConfig = true;
                    break;
                }
                for (String host : entry.hosts()) {
                    if (host.equals(ore.id.toString())) {
                        inHosts = true;
                    } else if (host.startsWith("#") && host.length() > 1) {
                        ResourceLocation tagLoc = ResourceLocation.tryParse(host.substring(1));
                        if (tagLoc != null && ore.block.builtInRegistryHolder().is(
                                TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tagLoc))) {
                            inHosts = true;
                        }
                    }
                }
            }
            String status = inConfig ? "[CONFIGURED]" : inHosts ? "[IN HOST]" : "[NOT CONFIGURED]";

            if (inConfig || inHosts) configuredCount++;
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

        sb.append("\n=== HOST TAG RESOLUTION ===\n");
        appendTagResolution(sb, registryAccess);

        sb.append("\n=== DIMENSION AUTO-DETECT ===\n");
        sb.append("Auto detect: ").append(OreConfig.isAutoDetectDimensionsEnabled() ? "ON" : "OFF").append("\n");
        if (OreConfig.getEnabledDimensions().isEmpty()) {
            sb.append("Extra dimensions: none detected yet (join a world to detect)\n");
        } else {
            sb.append("Extra dimensions:\n");
            for (String dimension : OreConfig.getEnabledDimensions()) {
                sb.append("  ").append(dimension).append("\n");
            }
        }
        sb.append("\n");

        sb.append("\n=== WORLDGEN / BIOME MODIFIERS ===\n");
        appendBiomeModifierInfo(sb);

        return sb.toString();
    }

    private static void appendTagResolution(StringBuilder sb, net.minecraft.core.RegistryAccess registryAccess) {
        Set<String> hostTags = new LinkedHashSet<>();
        for (OreEntry entry : OreConfig.allEntries()) {
            for (String host : entry.hosts()) {
                if (host.startsWith("#")) hostTags.add(host);
            }
        }
        if (hostTags.isEmpty()) {
            sb.append("No #-host tags configured.\n");
            return;
        }
        sb.append("Registry access: ").append(registryAccess != null ? "server" : "fallback (holder)").append("\n\n");
        for (String host : hostTags) {
            ResourceLocation tagLoc = ResourceLocation.tryParse(host.substring(1));
            if (tagLoc == null) {
                sb.append(host).append(" -> UNPARSEABLE\n");
                continue;
            }
            TagKey<Block> tag = TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tagLoc);
            sb.append(host).append("\n");
            for (String blockId : HOST_PROBE_BLOCKS) {
                sb.append("  ").append(blockId).append(": ")
                        .append(blockInTag(registryAccess, tag, blockId) ? "MEMBER" : "not member")
                        .append("\n");
            }
        }
        sb.append("\nentriesFor() probes (real matching code):\n");
        probeEntries(sb, registryAccess, "minecraft:stone", "minecraft:overworld");
        probeEntries(sb, registryAccess, "minecraft:deepslate", "minecraft:overworld");
        probeEntries(sb, registryAccess, "minecraft:netherrack", "minecraft:the_nether");
        probeEntries(sb, registryAccess, "minecraft:end_stone", "minecraft:the_end");

        sb.append("\n=== DATA PACK / RESOURCE DIAGNOSTIC ===\n");
        appendResourceDiagnostic(sb, registryAccess);
    }

    private static void appendResourceDiagnostic(StringBuilder sb, net.minecraft.core.RegistryAccess registryAccess) {
        if (registryAccess == null) {
            sb.append("No server registry access available (multiplayer); run in singleplayer.\n");
            return;
        }
        try {
            net.minecraft.server.MinecraftServer server = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer();
            if (server == null) {
                sb.append("No integrated server handle.\n");
                return;
            }
            sb.append("Selected data packs: ")
                    .append(server.getPackRepository().getSelectedIds())
                    .append("\n\n");

            net.minecraft.server.packs.resources.ResourceManager rm = server.getResourceManager();
            sb.append("Resource manager namespaces: ")
                    .append(new ArrayList<>(rm.getNamespaces()))
                    .append("\n");
            try {
                var listed = rm.listResources(BLOCK_TAG_DIRECTORY, p -> true);
                List<String> keys = new ArrayList<>();
                for (var e : listed.entrySet()) keys.add(String.valueOf(e.getKey()));
                sb.append("listResources(\"").append(BLOCK_TAG_DIRECTORY).append("\") - exactly what the tag loader iterates (")
                        .append(keys.size()).append(" files):\n");
                sb.append("  ").append(keys.isEmpty() ? "(none)" : String.join(", ", keys)).append("\n");
            } catch (Exception e) {
                sb.append("listResources failed: ").append(e).append("\n");
            }
            sb.append("\n");

            String[] tagFiles = {
                    blockTagFile("forge", "ore_bearing_stones.json"),
                    blockTagFile("forge", "overworld_ore_bearing_stones.json"),
                    blockTagFile("forge", "nether_ore_bearing_stones.json"),
                    blockTagFile("c", "ore_yield_probe.json"),
                    blockTagFile("ore_yield", "ore_yield_probe.json"),
                    blockTagFile("minecraft", "ore_yield_probe.json"),
                    "ore_yield:loot_modifiers/ore_yield.json"
            };
            for (String file : tagFiles) {
                ResourceLocation loc = ResourceLocation.tryParse(file);
                sb.append("file ").append(file).append(": ")
                        .append(loc != null && rm.getResource(loc).isPresent() ? "VISIBLE" : "NOT VISIBLE")
                        .append("\n");
            }

            sb.append("\nControl tags (vanilla, must be MEMBER if tag machinery works):\n");
            TagKey<Block> stoneReplaceable = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    ResourceLocation.tryParse("minecraft:stone_ore_replaceables"));
            sb.append("  minecraft:stone_ore_replaceables / stone: ")
                    .append(blockInTag(registryAccess, stoneReplaceable, "minecraft:stone") ? "MEMBER" : "not member")
                    .append("\n");
            TagKey<Block> dirtTag = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                    ResourceLocation.tryParse("minecraft:dirt"));
            sb.append("  minecraft:dirt / dirt: ")
                    .append(blockInTag(registryAccess, dirtTag, "minecraft:dirt") ? "MEMBER" : "not member")
                    .append("\n");

            sb.append("\nTags bound to minecraft:stone (server registry):\n");
            try {
                //? if registryaccess_lookup {
                var holder = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                        .getOrThrow(ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,
                                ResourceLocation.tryParse("minecraft:stone")));
                //?} else {
                var holder = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                        .getOrThrow(ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,
                                ResourceLocation.tryParse("minecraft:stone")));
                //?}
                List<String> tagList = new ArrayList<>();
                //? if registryaccess_lookup {
                holder.tags().forEach(t -> tagList.add(t.location().toString()));
                //?} else {
                holder.builtInRegistryHolder().tags().forEach(t -> tagList.add(t.location().toString()));
                //?}
                sb.append(tagList.isEmpty() ? "  (none)" : "  " + String.join(", ", tagList)).append("\n");
            } catch (Exception e) {
                sb.append("  holder lookup failed: ").append(e).append("\n");
            }

            sb.append("\nA/B probe tags (all ship stone as a value; from OUR jar):\n");
            String[] probeTags = {
                    "minecraft:ore_yield_probe",
                    "c:ore_yield_probe",
                    "ore_yield:ore_yield_probe",
                    "forge:ore_yield_probe"
            };
            for (String probe : probeTags) {
                TagKey<Block> tag = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                        ResourceLocation.tryParse(probe));
                String member = blockInTag(registryAccess, tag, "minecraft:stone") ? "MEMBER" : "not member";
                String reg;
                try {
                    //? if registryaccess_lookup {
                    var regTag = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                            .get(tag);
                    //?} else {
                    var regTag = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                            .getTag(tag);
                    //?}
                    reg = regTag.isEmpty() ? "TAG MISSING" : "present";
                } catch (Exception e) {
                    reg = "getTag error";
                }
                sb.append("  ").append(probe).append(": stone=").append(member)
                        .append(", registry=").append(reg).append("\n");
            }
            sb.append("\n");

            sb.append("\nMembers of #forge:overworld_ore_bearing_stones (via Registry#getTag):\n");
            try {
                //? if registryaccess_lookup {
                var tag = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                        .get(TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                                ResourceLocation.tryParse("forge:overworld_ore_bearing_stones")));
                //?} else {
                var tag = registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                        .getTag(TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
                                ResourceLocation.tryParse("forge:overworld_ore_bearing_stones")));
                //?}
                if (tag.isEmpty()) {
                    sb.append("  TAG MISSING (not present in registry tag set)\n");
                } else {
                    List<String> members = new ArrayList<>();
                    tag.get().stream().forEach(h -> h.unwrapKey().ifPresent(k -> {
                        //? if resourcekey_identifier {
                        members.add(k.identifier().toString());
                        //?} else {
                        members.add(k.location().toString());
                        //?}
                    }));
                    sb.append(members.isEmpty() ? "  EMPTY TAG" : "  " + String.join(", ", members)).append("\n");
                }
            } catch (Exception e) {
                sb.append("  getTag failed: ").append(e).append("\n");
            }

            sb.append("\n--- DEEPER RM PROBE ---\n");
            try {
                sb.append("RM class: ").append(rm.getClass().getName()).append("\n");
            } catch (Exception e) {
                sb.append("RM class: unknown (").append(e).append(")\n");
            }
            String[] controlFiles = {
                    blockTagFile("minecraft", "stone_ore_replaceables.json"),
                    blockTagFile("minecraft", "dirt.json"),
                    blockTagFile("c", "stones.json"),
                    "ore_yield:neoforge/biome_modifier/remove_ores.json"
            };
            for (String file : controlFiles) {
                ResourceLocation loc = ResourceLocation.tryParse(file);
                boolean visible = loc != null && rm.getResource(loc).isPresent();
                List<String> stack = new ArrayList<>();
                if (loc != null) {
                    try {
                        for (net.minecraft.server.packs.resources.Resource r : rm.getResourceStack(loc)) {
                            stack.add(r.sourcePackId());
                        }
                    } catch (Exception e) {
                        stack.add("stack err");
                    }
                }
                sb.append("probe ").append(file).append(": ")
                        .append(visible ? "VISIBLE" : "NOT VISIBLE")
                        .append("  packs=").append(stack.isEmpty() ? "(none)" : String.join(",", stack))
                        .append("\n");
            }

            sb.append("\nlistResources counts by directory:\n");
            for (String dir : new String[]{"tags", BLOCK_TAG_DIRECTORY, "loot_tables", "recipes", "neoforge/biome_modifier"}) {
                try {
                    int n = rm.listResources(dir, p -> true).size();
                    sb.append("  ").append(dir).append(": ").append(n).append("\n");
                } catch (Exception e) {
                    sb.append("  ").append(dir).append(": err ").append(e).append("\n");
                }
            }

            String overworldHostTag = blockTagFile("forge", "overworld_ore_bearing_stones.json");
            sb.append("\nraw bytes served for ").append(overworldHostTag).append(":\n");
            try {
                ResourceLocation loc = ResourceLocation.tryParse(overworldHostTag);
                var res = loc == null ? java.util.Optional.<net.minecraft.server.packs.resources.Resource>empty()
                        : rm.getResource(loc);
                if (res.isPresent()) {
                    String raw = new String(res.get().open().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    sb.append("  ").append(raw.length()).append(" chars, head: ")
                            .append(raw.substring(0, Math.min(250, raw.length())).replace("\r", "\\r").replace("\n", "\\n"))
                            .append("\n");
                } else {
                    sb.append("  not found\n");
                }
            } catch (Exception e) {
                sb.append("  err ").append(e).append("\n");
            }

            sb.append("\nDataPackContents loaded tags for BLOCK registry (raw, before binding):\n");
            try {
                Object srm = server.getClass().getMethod("getServerResourceManager").invoke(server);
                Object contents = srm.getClass().getMethod("getDataPackContents").invoke(srm);
                Object tagMap = contents.getClass().getMethod("getTags", net.minecraft.resources.ResourceKey.class)
                        .invoke(contents, net.minecraft.core.registries.Registries.BLOCK);
                List<String> keys = new ArrayList<>();
                for (Object k : ((java.util.Map<?, ?>) tagMap).keySet()) {
                    keys.add(String.valueOf(k));
                }
                java.util.Collections.sort(keys);
                sb.append("  total ").append(keys.size()).append(":\n  ")
                        .append(keys.isEmpty() ? "(none)" : String.join(", ", keys)).append("\n");
                String[] ours = {"forge:overworld_ore_bearing_stones", "forge:nether_ore_bearing_stones",
                        "forge:ore_bearing_stones", "minecraft:ore_yield_probe", "c:ore_yield_probe",
                        "ore_yield:ore_yield_probe", "forge:ore_yield_probe"};
                for (String t : ours) {
                    boolean present = keys.stream().anyMatch(k -> k.contains(t));
                    sb.append("  ours ").append(t).append(": ").append(present ? "LOADED" : "not loaded").append("\n");
                }
            } catch (Exception e) {
                sb.append("  DataPackContents probe failed: ").append(e).append("\n");
            }

            sb.append("\nRM internal pack list (field=value per pack holder):\n");
            try {
                Class<?> rmCls = rm.getClass();
                boolean found = false;
                for (java.lang.reflect.Field f : rmCls.getDeclaredFields()) {
                    if (!java.util.List.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    Object list = f.get(rm);
                    if (!(list instanceof java.util.List<?>)) continue;
                    for (Object holder : (java.util.List<?>) list) {
                        if (holder == null) continue;
                        found = true;
                        sb.append("  holder ");
                        for (java.lang.reflect.Field hf : holder.getClass().getDeclaredFields()) {
                            hf.setAccessible(true);
                            Object v = hf.get(holder);
                            sb.append(hf.getName()).append("=").append(describeHolderValue(v)).append(", ");
                        }
                        sb.append("\n");
                    }
                }
                if (!found) sb.append("  (no list field found on ").append(rmCls.getName()).append(")\n");
            } catch (Exception e) {
                sb.append("  pack probe failed: ").append(e).append("\n");
            }

            sb.append("\n--- CLIENT RM vs SERVER RM ---\n");
            try {
                net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                net.minecraft.server.packs.resources.ResourceManager clientRm = client.getResourceManager();
                sb.append("client RM class: ").append(clientRm.getClass().getName()).append("\n");
                String[] cf = {
                        blockTagFile("minecraft", "stone_ore_replaceables.json"),
                        blockTagFile("c", "stones.json"),
                        blockTagFile("forge", "overworld_ore_bearing_stones.json")
                };
                for (String file : cf) {
                    ResourceLocation loc = ResourceLocation.tryParse(file);
                    boolean visible = loc != null && clientRm.getResource(loc).isPresent();
                    List<String> stack = new ArrayList<>();
                    if (loc != null) {
                        try {
                            for (net.minecraft.server.packs.resources.Resource r : clientRm.getResourceStack(loc)) {
                                stack.add(r.sourcePackId());
                            }
                        } catch (Exception e) {
                            stack.add("stack err");
                        }
                    }
                    sb.append("client probe ").append(file).append(": ")
                            .append(visible ? "VISIBLE" : "NOT VISIBLE")
                            .append("  packs=").append(stack.isEmpty() ? "(none)" : String.join(",", stack))
                            .append("\n");
                }
                for (String dir : new String[]{"tags", BLOCK_TAG_DIRECTORY, ITEM_TAG_DIRECTORY, "tags/worldgen/biome", "loot_tables", "recipes"}) {
                    try {
                        int n = clientRm.listResources(dir, p -> true).size();
                        sb.append("client listResources(").append(dir).append("): ").append(n).append("\n");
                    } catch (Exception e) {
                        sb.append("client listResources(").append(dir).append("): err ").append(e).append("\n");
                    }
                }
            } catch (Exception e) {
                sb.append("client RM probe failed: ").append(e).append("\n");
            }

            sb.append("\nserver RM tag listing key analysis:\n");
            try {
                var all = rm.listResources("tags", p -> true);
                int mc = 0;
                List<String> first = new ArrayList<>();
                for (var e : all.entrySet()) {
                    if (e.getKey().getNamespace().equals("minecraft")) mc++;
                    if (first.size() < 20) first.add(String.valueOf(e.getKey()));
                }
                sb.append("  total ").append(all.size()).append(", minecraft-namespace ").append(mc).append("\n");
                sb.append("  first 20: ").append(String.join(", ", first)).append("\n");
                int items = rm.listResources(ITEM_TAG_DIRECTORY, p -> true).size();
                sb.append("  ").append(ITEM_TAG_DIRECTORY).append(": ").append(items).append("\n");
                int biomes = rm.listResources("tags/worldgen/biome", p -> true).size();
                sb.append("  tags/worldgen/biome: ").append(biomes).append("\n");
            } catch (Exception e) {
                sb.append("  key analysis failed: ").append(e).append("\n");
            }

            sb.append("\nTagManager getResult() - raw loaded tag maps (the actual reload output):\n");
            try {
                java.lang.reflect.Field resourcesField = server.getClass().getDeclaredField("resources");
                resourcesField.setAccessible(true);
                Object rr = resourcesField.get(server);
                Object rsl = rr.getClass().getMethod("managers").invoke(rr);
                Object tm;
                try {
                    tm = rsl.getClass().getMethod("getTagManager").invoke(rsl);
                } catch (Exception e) {
                    java.lang.reflect.Field f = rsl.getClass().getDeclaredField("tagManager");
                    f.setAccessible(true);
                    tm = f.get(rsl);
                }
                Object result = tm.getClass().getMethod("getResult").invoke(tm);
                for (Object lr : (java.util.List<?>) result) {
                    String key = String.valueOf(lr.getClass().getMethod("key").invoke(lr));
                    Object tags = lr.getClass().getMethod("tags").invoke(lr);
                    sb.append("  ").append(key).append(": ").append(((java.util.Map<?, ?>) tags).size())
                            .append(" tag(s)\n");
                    if (key.contains("BLOCK") || key.contains("block")) {
                        List<String> ks = new ArrayList<>();
                        for (Object k : ((java.util.Map<?, ?>) tags).keySet()) ks.add(String.valueOf(k));
                        java.util.Collections.sort(ks);
                        sb.append("    ").append(ks.isEmpty() ? "(none)" : String.join(", ", ks)).append("\n");
                        for (String t : new String[]{"forge:overworld_ore_bearing_stones", "minecraft:ore_yield_probe"}) {
                            boolean present = ks.stream().anyMatch(k -> k.contains(t));
                            sb.append("    ours ").append(t).append(": ").append(present ? "LOADED" : "not loaded").append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                sb.append("  tagManager probe failed: ").append(e).append("\n");
            }

            sb.append("\nclient world registry tags on stone (comparison):\n");
            try {
                net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                if (client.level != null) {
                    //? if registryaccess_lookup {
                    var holder = client.level.registryAccess()
                            .lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                            .getOrThrow(ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,
                                    ResourceLocation.tryParse("minecraft:stone")));
                    //?} else {
                    var holder = client.level.registryAccess()
                            .registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                            .getOrThrow(ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,
                                    ResourceLocation.tryParse("minecraft:stone")));
                    //?}
                    List<String> tagList = new ArrayList<>();
                    //? if registryaccess_lookup {
                    holder.tags().forEach(t -> tagList.add(t.location().toString()));
                    //?} else {
                    holder.builtInRegistryHolder().tags().forEach(t -> tagList.add(t.location().toString()));
                    //?}
                    sb.append("  ").append(tagList.isEmpty() ? "(none)" : String.join(", ", tagList)).append("\n");
                } else {
                    sb.append("  no client level\n");
                }
            } catch (Exception e) {
                sb.append("  client registry probe failed: ").append(e).append("\n");
            }
        } catch (Exception e) {
            sb.append("Diagnostic error: ").append(e).append("\n");
        }
        sb.append("\n");
    }

    private static String describeHolderValue(Object v) {
        if (v == null) return "null";
        try {
            if (v instanceof net.minecraft.server.packs.PackResources) {
                try {
                    Object id = v.getClass().getMethod("packId").invoke(v);
                    return String.valueOf(id);
                } catch (Exception e) {
                    return v.getClass().getName();
                }
            }
            if (v instanceof java.util.function.Predicate) {
                return "predicate:" + v;
            }
            return String.valueOf(v);
        } catch (Exception e) {
            return "err";
        }
    }

    private static boolean blockInTag(net.minecraft.core.RegistryAccess registryAccess, TagKey<Block> tag, String blockId) {
        ResourceLocation loc = ResourceLocation.tryParse(blockId);
        if (loc == null) return false;
        if (registryAccess != null) {
            try {
                //? if registryaccess_lookup {
                return registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                        .getOrThrow(ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, loc))
                        .is(tag);
                //?} else {
                return registryAccess.registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                        .getOrThrow(ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK, loc))
                        .defaultBlockState().is(tag);
                //?}
            } catch (Exception e) {
                return false;
            }
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(loc).orElse(null);
        return block != null && block.builtInRegistryHolder().is(tag);
    }

    private static void probeEntries(StringBuilder sb, net.minecraft.core.RegistryAccess registryAccess,
                                     String blockId, String dimension) {
        Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.tryParse(blockId)).orElse(null);
        if (block == null) {
            sb.append("  ").append(blockId).append(": BLOCK NOT IN REGISTRY\n");
            return;
        }
        BlockState state = block.defaultBlockState();
        List<OreEntry> hits = OreConfig.entriesFor(state, dimension);
        sb.append("  ").append(blockId).append(" (").append(dimension).append("): ")
                .append(hits.isEmpty() ? "NO MATCHES" : hits.size() + " match(es): "
                        + hits.stream().map(OreEntry::id).sorted().toList())
                .append("\n");
    }

    private static List<OreBlockInfo> scanRegistryForOres() {
        List<OreBlockInfo> result = new ArrayList<>();
        BuiltInRegistries.BLOCK.forEach(block -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) return;

            boolean isOre = id.getPath().endsWith("_ore")
                    || block.builtInRegistryHolder().is(ORE_TAG);

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
        ResourceLocation rawId = ResourceLocations.of(blockId.getNamespace(), "raw_" + path(blockId));
        if (BuiltInRegistries.ITEM.containsKey(rawId)) return rawId.toString();

        ResourceLocation gemId = ResourceLocations.of(blockId.getNamespace(), path(blockId).replace("_ore", ""));
        if (BuiltInRegistries.ITEM.containsKey(gemId)) return gemId.toString();

        ResourceLocation nuggetId = ResourceLocations.of(blockId.getNamespace(), path(blockId).replace("_ore", "") + "_nugget");
        if (BuiltInRegistries.ITEM.containsKey(nuggetId)) return nuggetId.toString();

        return blockId.toString() + " (drops itself)";
    }

    private static String path(ResourceLocation id) {
        return id.getPath();
    }

    private static String blockTagFile(String namespace, String fileName) {
        return namespace + ":" + BLOCK_TAG_DIRECTORY + "/" + fileName;
    }

    private static void appendBiomeModifierInfo(StringBuilder sb) {
        sb.append("Biome modifiers control vanilla ore removal (when enabled in config).\n");
        sb.append("To see ore Y-ranges in-game, use F3 debug screen while mining,\n");
        sb.append("or run /locate structure to find ore veins.\n");
        sb.append("For detailed worldgen data, run: gradlew :forge:runData and check forge/src/generated/resources/.\n");
    }

    private record OreBlockInfo(ResourceLocation id, Block block, List<String> tags, String drops) {}
}
