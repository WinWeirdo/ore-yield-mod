package com.oreyield.config;

import net.minecraft.ChatFormatting;
//? if screen_extract_render_state {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Local TOML editor only. It deliberately does not synchronize or modify a remote
 * multiplayer server's authoritative settings.
 */
public final class OreYieldConfigScreen extends Screen {
    //? if screen_extract_render_state {
    private static final int PRIMARY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0xFFAAAAAA;
    private static final int HEADING_TEXT_COLOR = 0xFFFFD35C;
    private static final int ERROR_TEXT_COLOR = 0xFFFF5555;
    //?} else if gui_graphics_argb {
    private static final int PRIMARY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0xFFAAAAAA;
    private static final int HEADING_TEXT_COLOR = 0xFFFFD35C;
    private static final int ERROR_TEXT_COLOR = 0xFFFF5555;
    //?} else {
    private static final int PRIMARY_TEXT_COLOR = 0xFFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0xAAAAAA;
    private static final int HEADING_TEXT_COLOR = 0xFFD35C;
    private static final int ERROR_TEXT_COLOR = 0xFF5555;
    //?}

    private static final int ROW_HEIGHT = 24;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_WIDTH = 82;
    private static final int SIDEBAR_MIN_WIDTH = 102;
    private static final int SIDEBAR_MAX_WIDTH = 138;

    private enum Category {
        GENERAL("general"),
        ORES("ores"),
        GENERATORS("generators"),
        COMPATIBILITY("compatibility"),
        MINERAL_POCKETS("mineral_pockets"),
        WORLD_GENERATION("world_generation"),
        ADVANCED("advanced");

        private final String key;

        Category(String key) {
            this.key = key;
        }

        Component title() {
            return Component.translatable("gui.ore_yield.category." + key);
        }

        Component help() {
            return Component.translatable("gui.ore_yield.category." + key + ".help");
        }
    }

    private record LayoutLabel(Component text, int x, int y, boolean heading) {
    }

    private record HoverHelp(int x, int y, int width, int height, Component text) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private final Screen parent;
    private final Map<String, Boolean> pendingFlags = new LinkedHashMap<>();
    private final Map<String, String> pendingText = new LinkedHashMap<>();
    private final Map<String, Boolean> initialFlags = new LinkedHashMap<>();
    private final Map<String, String> initialText = new LinkedHashMap<>();
    private final Map<String, EditBox> visibleFields = new LinkedHashMap<>();
    private final List<LayoutLabel> labels = new ArrayList<>();
    private final List<HoverHelp> hoverHelp = new ArrayList<>();

    private Category category = Category.GENERAL;
    private OreEntry selectedOre;
    private MineralPocketType selectedPocketType = MineralPocketType.COAL;
    private boolean draftLoaded;
    private int sidebarX;
    private int sidebarWidth;
    private int contentX;
    private int contentWidth;
    private int contentTop;
    private int contentBottom;
    private int contentY;
    private int contentScroll;
    private int maxContentScroll;
    private Component validationMessage;

    public OreYieldConfigScreen(Screen parent) {
        super(Component.translatable("gui.ore_yield.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (!draftLoaded) loadDraft();
        rebuildScreen();
    }

    private void loadDraft() {
        pendingFlags.clear();
        pendingText.clear();
        putFlag("bad_luck_eliminator", OreConfig.isBadLuckEliminatorEnabled());
        putFlag("auto_detect_dimensions", OreConfig.isAutoDetectDimensionsEnabled());
        putFlag("remove_vanilla_ore_generation", OreConfig.shouldRemoveVanillaOreGeneration());
        putFlag("remove_compatible_ore_generation", OreConfig.shouldRemoveCompatibleOreGeneration());
        putFlag("enable_mod_compat", OreConfig.isModCompatEnabled());
        putFlag("enable_mod_compat_2", OreConfig.isModCompat2Enabled());
        putFlag("mod_compat_2_ores_in_end", OreConfig.isModCompat2OresInEnd());
        putFlag("enable_vanilla_end_ores", OreConfig.isVanillaEndOresEnabled());
        putFlag("generator_ore_yield_enabled", OreConfig.isGeneratorOreYieldEnabled());
        putFlag("enable_anti_cheese_mechanics", OreConfig.isAntiCheeseMechanicsEnabled());
        putFlag("allow_player_placed_eligible_blocks", OreConfig.allowsPlayerPlacedEligibleBlocks());
        putFlag("allow_generator_automated_harvesting", OreConfig.allowsGeneratorAutomatedHarvesting());
        putFlag("allow_generator_explosion_harvesting", OreConfig.allowsGeneratorExplosionHarvesting());
        putFlag("enable_mineral_pockets", OreConfig.isMineralPocketsEnabled());
        putFlag("mineral_pockets_end_enabled", OreConfig.isMineralPocketsInEndEnabled());
        putFlag("mineral_pockets_allow_generator", OreConfig.allowsMineralPocketsOnGenerator());
        putFlag("mineral_pockets_allow_automated_harvesting", OreConfig.allowsMineralPocketAutomatedHarvesting());

        putText("bad_luck_multiplier", String.valueOf(OreConfig.badLuckMultiplier()));
        putText("stone_generator_cooldown_ticks", String.valueOf(OreConfig.stoneGeneratorCooldownTicks()));
        putText("generator_ore_yield_chance_multiplier", String.valueOf(OreConfig.generatorOreYieldChanceMultiplier()));
        putText("mineral_pocket_chance", formatPercent(OreConfig.mineralPocketChance()));
        putText("mineral_pocket_min_resource_types", String.valueOf(OreConfig.mineralPocketMinResourceTypes()));
        putText("mineral_pocket_max_resource_types", String.valueOf(OreConfig.mineralPocketMaxResourceTypes()));
        for (MineralPocketType type : MineralPocketType.values()) {
            MineralPocketSettings settings = OreConfig.mineralPocketSettings(type);
            String prefix = pocketPrefix(type);
            putFlag(prefix + "enabled", settings.enabled());
            putText(prefix + "weight", String.valueOf(settings.weight()));
            putText(prefix + "min_count", String.valueOf(settings.minCount()));
            putText(prefix + "max_count", String.valueOf(settings.maxCount()));
        }
        initialFlags.clear();
        initialFlags.putAll(pendingFlags);
        initialText.clear();
        initialText.putAll(pendingText);
        draftLoaded = true;
    }

    private void putFlag(String key, boolean value) {
        pendingFlags.put(key, value);
    }

    private void putText(String key, String value) {
        pendingText.put(key, value);
    }

    private void captureVisibleFields() {
        for (Map.Entry<String, EditBox> entry : visibleFields.entrySet()) {
            pendingText.put(entry.getKey(), entry.getValue().getValue());
        }
        visibleFields.clear();
    }

    private void rebuildScreen() {
        captureVisibleFields();
        this.clearWidgets();
        labels.clear();
        hoverHelp.clear();

        int margin = Math.max(10, this.width / 48);
        sidebarX = margin;
        sidebarWidth = Math.max(SIDEBAR_MIN_WIDTH, Math.min(SIDEBAR_MAX_WIDTH, this.width / 4));
        contentX = sidebarX + sidebarWidth + 12;
        contentWidth = Math.max(120, this.width - contentX - margin);
        contentTop = 48;
        contentBottom = Math.max(contentTop + 42, this.height - 54);
        maxContentScroll = maximumContentScroll();
        contentScroll = Math.max(0, Math.min(contentScroll, maxContentScroll));

        addLabel(Component.translatable("gui.ore_yield.config.navigation"), sidebarX, 30, true);
        int navigationY = 45;
        for (Category target : Category.values()) {
            Button button = addButton(target.title(), sidebarX, navigationY, sidebarWidth, CONTROL_HEIGHT, btn -> {
                validationMessage = null;
                category = target;
                selectedOre = null;
                contentScroll = 0;
                rebuildScreen();
            }, target.help());
            button.active = category != target || selectedOre != null;
            navigationY += CONTROL_HEIGHT + 3;
        }

        if (selectedOre != null) {
            buildOreDetail();
        } else {
            switch (category) {
                case GENERAL -> buildGeneral();
                case ORES -> buildOreList();
                case GENERATORS -> buildGenerators();
                case COMPATIBILITY -> buildCompatibility();
                case MINERAL_POCKETS -> buildMineralPockets();
                case WORLD_GENERATION -> buildWorldGeneration();
                case ADVANCED -> buildAdvanced();
            }
        }
        buildFooter(margin);
    }

    private void beginSection(Component sectionTitle, Component description) {
        addLabel(sectionTitle, contentX, 31, true);
        addLabel(description, contentX, 43, false);
        contentY = contentTop + 23 - contentScroll;
        if (maxContentScroll > 0) {
            addButton(Component.translatable("gui.ore_yield.config.scroll_up"), contentX + contentWidth - 84, 28, 40, 16,
                    btn -> scrollContent(-ROW_HEIGHT * 3), Component.translatable("gui.ore_yield.config.scroll_up.help"));
            addButton(Component.translatable("gui.ore_yield.config.scroll_down"), contentX + contentWidth - 41, 28, 40, 16,
                    btn -> scrollContent(ROW_HEIGHT * 3), Component.translatable("gui.ore_yield.config.scroll_down.help"));
        }
    }

    private void buildGeneral() {
        beginSection(Category.GENERAL.title(), Category.GENERAL.help());
        addToggle("bad_luck_eliminator", "bad_luck_eliminator");
        addTextField("bad_luck_multiplier", "bad_luck_multiplier", 12);
        addToggle("auto_detect_dimensions", "auto_detect_dimensions");
        addInfoRow(Component.translatable("gui.ore_yield.config.local_only"));
    }

    private void buildGenerators() {
        beginSection(Category.GENERATORS.title(), Category.GENERATORS.help());
        addToggle("enable_anti_cheese_mechanics", "enable_anti_cheese_mechanics");
        addInfoRow(Component.translatable("gui.ore_yield.config.anti_cheese.restart_notice"));
        addToggle("generator_ore_yield_enabled", "generator_ore_yield_enabled");
        addTextField("generator_ore_yield_chance_multiplier", "generator_ore_yield_chance_multiplier", 12);
        addTextField("stone_generator_cooldown_ticks", "stone_generator_cooldown_ticks", 12);
        addToggle("allow_player_placed_eligible_blocks", "allow_player_placed_eligible_blocks");
        addToggle("allow_generator_automated_harvesting", "allow_generator_automated_harvesting");
        addToggle("allow_generator_explosion_harvesting", "allow_generator_explosion_harvesting");
    }

    private void buildCompatibility() {
        beginSection(Category.COMPATIBILITY.title(), Category.COMPATIBILITY.help());
        addToggle("enable_mod_compat", "enable_mod_compat");
        addToggle("enable_mod_compat_2", "enable_mod_compat_2");
        addToggle("mod_compat_2_ores_in_end", "mod_compat_2_ores_in_end");
        addToggle("enable_vanilla_end_ores", "enable_vanilla_end_ores");
        addInfoRow(Component.translatable("gui.ore_yield.config.compatibility.restart_notice"));
    }

    private void buildMineralPockets() {
        beginSection(Category.MINERAL_POCKETS.title(), Category.MINERAL_POCKETS.help());
        addToggle("enable_mineral_pockets", "enable_mineral_pockets");
        addToggle("mineral_pockets_end_enabled", "mineral_pockets_end_enabled");
        addToggle("mineral_pockets_allow_generator", "mineral_pockets_allow_generator");
        addToggle("mineral_pockets_allow_automated_harvesting", "mineral_pockets_allow_automated_harvesting");
        // The shipped probability is rendered as scientific notation by Double.toString
        // and needs more than the ordinary short-number field limit.
        addTextField("mineral_pocket_chance", "mineral_pocket_chance", 32);
        addTextField("mineral_pocket_min_resource_types", "mineral_pocket_min_resource_types", 6);
        addTextField("mineral_pocket_max_resource_types", "mineral_pocket_max_resource_types", 6);
        addChoiceRow("mineral_pocket_type", pocketTypeName(selectedPocketType), this::nextPocketType);
        String prefix = pocketPrefix(selectedPocketType);
        addToggle(prefix + "enabled", "mineral_pocket_category_enabled");
        addTextField(prefix + "weight", "mineral_pocket_category_weight", 10);
        addTextField(prefix + "min_count", "mineral_pocket_category_min_count", 6);
        addTextField(prefix + "max_count", "mineral_pocket_category_max_count", 6);
    }

    private void buildWorldGeneration() {
        beginSection(Category.WORLD_GENERATION.title(), Category.WORLD_GENERATION.help());
        addToggle("remove_vanilla_ore_generation", "remove_vanilla_ore_generation");
        addToggle("remove_compatible_ore_generation", "remove_compatible_ore_generation");
        addInfoRow(Component.translatable("gui.ore_yield.config.world_generation.new_chunks_only"));
    }

    private void buildAdvanced() {
        beginSection(Category.ADVANCED.title(), Category.ADVANCED.help());
        addInfoRow(Component.translatable("gui.ore_yield.config.advanced.file_only"));
        String path = OreConfig.configPath() == null ? "config/ore_yield.toml" : OreConfig.configPath().toString();
        addInfoRow(Component.literal(path).withStyle(ChatFormatting.GRAY));
        addInfoRow(Component.translatable("gui.ore_yield.config.advanced.cancel_notice"));
    }

    private void buildOreList() {
        beginSection(Category.ORES.title(), Category.ORES.help());
        addSearchField();
        List<OreEntry> ores = filteredOres();
        if (ores.isEmpty()) {
            addInfoRow(Component.translatable("gui.ore_yield.config.ores.empty"));
            return;
        }
        for (OreEntry entry : ores) {
            int y = nextRow();
            if (!isVisibleRow(y)) continue;
            Component row = Component.translatable(entry.enabled()
                            ? "gui.ore_yield.config.ores.row.enabled"
                            : "gui.ore_yield.config.ores.row.disabled",
                    Component.literal(readableOreName(entry.id())), Component.literal(oreNamespace(entry.id())),
                    Component.literal(entry.dimension()), Component.literal(formatChance(entry.chance())),
                    Component.literal(String.valueOf(entry.minY())), Component.literal(String.valueOf(entry.peakY())),
                    Component.literal(String.valueOf(entry.maxY())));
            Button button = addButton(row, contentX, y, contentWidth, CONTROL_HEIGHT, btn -> openOre(entry),
                    Component.translatable("gui.ore_yield.config.ores.row_help", Component.literal(entry.id())));
            button.active = OreConfig.effectiveEntry(entry.id()) != null;
            if (!button.active) {
                addHelp(contentX, y, contentWidth, CONTROL_HEIGHT,
                        Component.translatable("gui.ore_yield.config.ores.read_only"));
            }
        }
    }

    private void addSearchField() {
        int y = nextRow();
        if (!isVisibleRow(y)) return;
        addLabel(Component.translatable("gui.ore_yield.config.ores.search"), contentX, y + 5, false);
        EditBox field = new EditBox(this.font, contentX + 92, y, Math.max(54, contentWidth - 178), CONTROL_HEIGHT,
                Component.translatable("gui.ore_yield.config.ores.search"));
        field.setMaxLength(96);
        field.setValue(pendingText.getOrDefault("ore_search", ""));
        this.addRenderableWidget(field);
        visibleFields.put("ore_search", field);
        addButton(Component.translatable("gui.ore_yield.config.ores.search_apply"), contentX + contentWidth - 80, y, 80,
                CONTROL_HEIGHT, btn -> {
                    captureVisibleFields();
                    contentScroll = 0;
                    rebuildScreen();
                }, Component.translatable("gui.ore_yield.config.ores.search.help"));
        addHelp(contentX, y, contentWidth, CONTROL_HEIGHT, Component.translatable("gui.ore_yield.config.ores.search.help"));
    }

    private void buildOreDetail() {
        beginSection(Component.translatable("gui.ore_yield.config.ore_editor", Component.literal(readableOreName(selectedOre.id()))),
                Component.translatable("gui.ore_yield.config.ore_editor.help", Component.literal(selectedOre.id())));
        addToggle("ore.enabled", "ore.enabled");
        addTextField("ore.result_item", "ore.result_item", 96);
        addTextField("ore.host_blocks", "ore.host_blocks", 192);
        addTextField("ore.min_count", "ore.min_count", 6);
        addTextField("ore.max_count", "ore.max_count", 6);
        addTextField("ore.chance", "ore.chance", 14);
        addTextField("ore.min_y", "ore.min_y", 8);
        addTextField("ore.peak_y", "ore.peak_y", 8);
        addTextField("ore.max_y", "ore.max_y", 8);
        addChoiceRow("ore.fortune_type", Component.literal(pendingText.getOrDefault("ore.fortune_type", FortuneType.NONE.name())), this::nextFortuneType);
        addTextField("ore.xp_min", "ore.xp_min", 6);
        addTextField("ore.xp_max", "ore.xp_max", 6);
        addTextField("ore.dimension", "ore.dimension", 48);
        addTextField("ore.min_pickaxe_level", "ore.min_pickaxe_level", 4);

        int y = nextRow();
        if (isVisibleRow(y)) {
            addButton(Component.translatable("gui.ore_yield.config.ore_editor.back"), contentX, y, 112, CONTROL_HEIGHT, btn -> {
                selectedOre = null;
                contentScroll = 0;
                rebuildScreen();
            }, Component.translatable("gui.ore_yield.config.ore_editor.back.help"));
        }
    }

    private void addToggle(String key, String labelKey) {
        int y = nextRow();
        if (!isVisibleRow(y)) return;
        Component help = Component.translatable("gui.ore_yield.help." + labelKey);
        addLabel(Component.translatable("gui.ore_yield.setting." + labelKey), contentX, y + 5, false);
        Button button = addButton(toggleMessage(pendingFlags.getOrDefault(key, false)), controlX(), y, CONTROL_WIDTH, CONTROL_HEIGHT,
                btn -> {
                    boolean value = !pendingFlags.getOrDefault(key, false);
                    pendingFlags.put(key, value);
                    btn.setMessage(toggleMessage(value));
                }, help);
        addHelp(contentX, y, contentWidth, CONTROL_HEIGHT, help);
    }

    private void addTextField(String key, String labelKey, int maxLength) {
        int y = nextRow();
        if (!isVisibleRow(y)) return;
        Component help = Component.translatable("gui.ore_yield.help." + labelKey);
        addLabel(Component.translatable("gui.ore_yield.setting." + labelKey), contentX, y + 5, false);
        EditBox field = new EditBox(this.font, fieldX(), y, fieldWidth(), CONTROL_HEIGHT,
                Component.translatable("gui.ore_yield.setting." + labelKey));
        field.setMaxLength(maxLength);
        field.setValue(pendingText.getOrDefault(key, ""));
        this.addRenderableWidget(field);
        visibleFields.put(key, field);
        addHelp(contentX, y, contentWidth, CONTROL_HEIGHT, help);
    }

    private void addChoiceRow(String labelKey, Component current, Runnable next) {
        int y = nextRow();
        if (!isVisibleRow(y)) return;
        Component help = Component.translatable("gui.ore_yield.help." + labelKey);
        addLabel(Component.translatable("gui.ore_yield.setting." + labelKey), contentX, y + 5, false);
        addButton(current, fieldX(), y, fieldWidth(), CONTROL_HEIGHT, btn -> {
            next.run();
            rebuildScreen();
        }, help);
        addHelp(contentX, y, contentWidth, CONTROL_HEIGHT, help);
    }

    private void addInfoRow(Component text) {
        int y = nextRow();
        if (isVisibleRow(y)) addLabel(text, contentX, y + 5, false);
    }

    private Button addButton(Component text, int x, int y, int width, int height, Button.OnPress onPress, Component help) {
        Button button = Button.builder(text, onPress).bounds(x, y, width, height).build();
        this.addRenderableWidget(button);
        addHelp(x, y, width, height, help);
        return button;
    }

    private void addLabel(Component text, int x, int y, boolean heading) {
        labels.add(new LayoutLabel(text, x, y, heading));
    }

    private void addHelp(int x, int y, int width, int height, Component text) {
        hoverHelp.add(new HoverHelp(x, y, width, height, text));
    }

    private void buildFooter(int margin) {
        int y = this.height - 28;
        int buttonWidth = Math.max(42, Math.min(96, (this.width - margin * 2 - 24) / 5));
        int x = this.width - margin - buttonWidth * 5 - 24;
        addButton(Component.translatable("gui.ore_yield.config.reload"), x, y, buttonWidth, CONTROL_HEIGHT, btn -> {
            validationMessage = null;
            OreConfig.reloadFromFile();
            selectedOre = null;
            contentScroll = 0;
            loadDraft();
            rebuildScreen();
        }, Component.translatable("gui.ore_yield.config.reload.help"));
        x += buttonWidth + 6;
        addButton(Component.translatable("gui.ore_yield.config.reset_section"), x, y, buttonWidth, CONTROL_HEIGHT, btn -> {
            resetCurrentSection();
            rebuildScreen();
        }, Component.translatable("gui.ore_yield.config.reset_section.help"));
        x += buttonWidth + 6;
        addButton(Component.translatable("gui.ore_yield.config.cancel"), x, y, buttonWidth, CONTROL_HEIGHT,
                btn -> closeToParent(), Component.translatable("gui.ore_yield.config.cancel.help"));
        x += buttonWidth + 6;
        addButton(Component.translatable("gui.ore_yield.config.save"), x, y, buttonWidth, CONTROL_HEIGHT,
                btn -> saveChanges(false), Component.translatable("gui.ore_yield.config.save.help"));
        x += buttonWidth + 6;
        addButton(Component.translatable("gui.ore_yield.config.save_close"), x, y, buttonWidth, CONTROL_HEIGHT,
                btn -> saveChanges(true), Component.translatable("gui.ore_yield.config.save_close.help"));
    }

    private void resetCurrentSection() {
        captureVisibleFields();
        switch (category) {
            case GENERAL -> resetKeys("bad_luck_eliminator", "auto_detect_dimensions", "bad_luck_multiplier");
            case GENERATORS -> resetKeys("generator_ore_yield_enabled", "generator_ore_yield_chance_multiplier",
                    "enable_anti_cheese_mechanics", "stone_generator_cooldown_ticks", "allow_player_placed_eligible_blocks",
                    "allow_generator_automated_harvesting", "allow_generator_explosion_harvesting");
            case COMPATIBILITY -> resetKeys("enable_mod_compat", "enable_mod_compat_2", "mod_compat_2_ores_in_end", "enable_vanilla_end_ores");
            case MINERAL_POCKETS -> resetPocketSection();
            case WORLD_GENERATION -> resetKeys("remove_vanilla_ore_generation", "remove_compatible_ore_generation");
            case ORES -> {
                if (selectedOre != null) populateOreDraft(selectedOre);
                else pendingText.put("ore_search", "");
            }
            case ADVANCED -> {
                // Read-only section: there is no staged setting to reset.
            }
        }
        validationMessage = Component.translatable("gui.ore_yield.config.reset_section.done");
    }

    private void resetKeys(String... keys) {
        for (String key : keys) {
            if (initialFlags.containsKey(key)) pendingFlags.put(key, initialFlags.get(key));
            if (initialText.containsKey(key)) pendingText.put(key, initialText.get(key));
        }
    }

    private void resetPocketSection() {
        resetKeys("enable_mineral_pockets", "mineral_pockets_end_enabled", "mineral_pockets_allow_generator",
                "mineral_pockets_allow_automated_harvesting", "mineral_pocket_chance",
                "mineral_pocket_min_resource_types", "mineral_pocket_max_resource_types");
        for (MineralPocketType type : MineralPocketType.values()) {
            String prefix = pocketPrefix(type);
            resetKeys(prefix + "enabled", prefix + "weight", prefix + "min_count", prefix + "max_count");
        }
    }

    private void saveChanges(boolean closeAfterSave) {
        captureVisibleFields();
        if (!validateGlobalFields() || !validateOreFields()) {
            rebuildScreen();
            return;
        }

        for (Map.Entry<String, Boolean> entry : pendingFlags.entrySet()) {
            if (!entry.getKey().startsWith("ore.") && !entry.getKey().startsWith("mineral_pocket_")) {
                OreConfig.setValue(entry.getKey(), entry.getValue());
            }
        }
        OreConfig.setValue("bad_luck_multiplier", number("bad_luck_multiplier"));
        OreConfig.setValue("generator_ore_yield_chance_multiplier", number("generator_ore_yield_chance_multiplier"));
        OreConfig.setStoneGeneratorCooldownTicks(integer("stone_generator_cooldown_ticks"));
        OreConfig.setMineralPocketChance(number("mineral_pocket_chance") / 100.0D);
        OreConfig.setMineralPocketResourceTypeRange(integer("mineral_pocket_min_resource_types"), integer("mineral_pocket_max_resource_types"));
        for (MineralPocketType type : MineralPocketType.values()) {
            String prefix = pocketPrefix(type);
            OreConfig.setMineralPocketSettings(type, pendingFlags.getOrDefault(prefix + "enabled", true),
                    integer(prefix + "weight"), integer(prefix + "min_count"), integer(prefix + "max_count"));
        }
        if (selectedOre != null) applyOreFields();
        OreConfig.saveAndRebuild();
        loadDraft();
        validationMessage = Component.translatable("gui.ore_yield.config.saved");
        if (closeAfterSave) {
            closeToParent();
        } else {
            rebuildScreen();
        }
    }

    private boolean validateGlobalFields() {
        return validateDouble("bad_luck_multiplier", 1.0D, 100.0D)
                && validateDouble("generator_ore_yield_chance_multiplier", 0.0D, 100.0D)
                && validateInteger("stone_generator_cooldown_ticks", 1, 1_728_000)
                && validateDouble("mineral_pocket_chance", 0.0D, 100.0D)
                && validateInteger("mineral_pocket_min_resource_types", 1, 16)
                && validateInteger("mineral_pocket_max_resource_types", integerOrDefault("mineral_pocket_min_resource_types", 1), 16)
                && validatePocketFields();
    }

    private boolean validatePocketFields() {
        for (MineralPocketType type : MineralPocketType.values()) {
            String prefix = pocketPrefix(type);
            if (!validateInteger(prefix + "weight", 0, 1_000_000)
                    || !validateInteger(prefix + "min_count", 1, 64)
                    || !validateInteger(prefix + "max_count", integerOrDefault(prefix + "min_count", 1), 64)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateOreFields() {
        if (selectedOre == null) return true;
        String item = pendingText.getOrDefault("ore.result_item", "");
        String hosts = pendingText.getOrDefault("ore.host_blocks", "");
        String dimension = pendingText.getOrDefault("ore.dimension", "");
        String fortune = pendingText.getOrDefault("ore.fortune_type", FortuneType.NONE.name());
        if (!validResourceId(item) || !validHostList(hosts) || (!dimension.isEmpty() && !validResourceId(dimension))) {
            return invalid("gui.ore_yield.validation.invalid_id");
        }
        try {
            FortuneType.valueOf(fortune);
        } catch (IllegalArgumentException ignored) {
            return invalid("gui.ore_yield.validation.invalid_fortune");
        }
        return validateInteger("ore.min_count", 1, 64)
                && validateInteger("ore.max_count", integerOrDefault("ore.min_count", 1), 64)
                && validateDouble("ore.chance", 0.0D, 1.0D)
                && validateInteger("ore.min_y", -2048, 2048)
                && validateInteger("ore.max_y", integerOrDefault("ore.min_y", -2048), 2048)
                && validatePeakY()
                && validateInteger("ore.xp_min", 0, 128)
                && validateInteger("ore.xp_max", integerOrDefault("ore.xp_min", 0), 128)
                && validateInteger("ore.min_pickaxe_level", 0, 3);
    }

    private boolean validatePeakY() {
        if (!validateInteger("ore.peak_y", -1, 2048)) return false;
        int peak = integer("ore.peak_y");
        if (peak == -1 || (peak >= integer("ore.min_y") && peak <= integer("ore.max_y"))) return true;
        return invalid("gui.ore_yield.validation.invalid_range");
    }

    private boolean validateDouble(String key, double minimum, double maximum) {
        try {
            double value = number(key);
            if (Double.isFinite(value) && value >= minimum && value <= maximum) return true;
        } catch (NumberFormatException ignored) {
        }
        return invalid("gui.ore_yield.validation.invalid_number");
    }

    private boolean validateInteger(String key, int minimum, int maximum) {
        try {
            int value = integer(key);
            if (value >= minimum && value <= maximum) return true;
        } catch (NumberFormatException ignored) {
        }
        return invalid("gui.ore_yield.validation.invalid_integer");
    }

    private boolean invalid(String translationKey) {
        validationMessage = Component.translatable(translationKey);
        return false;
    }

    private void applyOreFields() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("enabled", String.valueOf(pendingFlags.getOrDefault("ore.enabled", true)));
        values.put("host_blocks", tomlHosts(pendingText.getOrDefault("ore.host_blocks", "")));
        values.put("result_item", quote(pendingText.getOrDefault("ore.result_item", "")));
        values.put("min_count", pendingText.get("ore.min_count"));
        values.put("max_count", pendingText.get("ore.max_count"));
        values.put("chance", pendingText.get("ore.chance"));
        values.put("min_y", pendingText.get("ore.min_y"));
        values.put("max_y", pendingText.get("ore.max_y"));
        values.put("peak_y", pendingText.get("ore.peak_y"));
        values.put("fortune_type", quote(pendingText.get("ore.fortune_type")));
        values.put("xp_min", pendingText.get("ore.xp_min"));
        values.put("xp_max", pendingText.get("ore.xp_max"));
        values.put("dimension", quote(pendingText.get("ore.dimension")));
        values.put("min_pickaxe_level", pendingText.get("ore.min_pickaxe_level"));
        OreConfig.applyOverrides(selectedOre.id(), values);
    }

    private void openOre(OreEntry entry) {
        if (OreConfig.effectiveEntry(entry.id()) == null) {
            validationMessage = Component.translatable("gui.ore_yield.config.ores.read_only");
            return;
        }
        selectedOre = entry;
        populateOreDraft(entry);
        contentScroll = 0;
        rebuildScreen();
    }

    private void populateOreDraft(OreEntry entry) {
        OreEntry value = OreConfig.effectiveEntry(entry.id());
        if (value == null) value = entry;
        pendingFlags.put("ore.enabled", value.enabled());
        pendingText.put("ore.result_item", value.resultItem());
        pendingText.put("ore.host_blocks", String.join(", ", value.hosts()));
        pendingText.put("ore.min_count", String.valueOf(value.minCount()));
        pendingText.put("ore.max_count", String.valueOf(value.maxCount()));
        pendingText.put("ore.chance", String.valueOf(value.chance()));
        pendingText.put("ore.min_y", String.valueOf(value.minY()));
        pendingText.put("ore.peak_y", String.valueOf(value.peakY()));
        pendingText.put("ore.max_y", String.valueOf(value.maxY()));
        pendingText.put("ore.fortune_type", value.fortuneType().name());
        pendingText.put("ore.xp_min", String.valueOf(value.xpMin()));
        pendingText.put("ore.xp_max", String.valueOf(value.xpMax()));
        pendingText.put("ore.dimension", value.dimension());
        pendingText.put("ore.min_pickaxe_level", String.valueOf(value.minPickaxeLevel()));
    }

    private void nextPocketType() {
        MineralPocketType[] values = MineralPocketType.values();
        selectedPocketType = values[(selectedPocketType.ordinal() + 1) % values.length];
    }

    private void nextFortuneType() {
        FortuneType[] values = FortuneType.values();
        FortuneType current = FortuneType.parse(pendingText.getOrDefault("ore.fortune_type", FortuneType.NONE.name()));
        pendingText.put("ore.fortune_type", values[(current.ordinal() + 1) % values.length].name());
    }

    private List<OreEntry> filteredOres() {
        String search = pendingText.getOrDefault("ore_search", "").strip().toLowerCase(Locale.ROOT);
        List<OreEntry> ores = new ArrayList<>();
        for (OreEntry entry : OreConfig.allEntries()) {
            String haystack = entry.id().toLowerCase(Locale.ROOT) + " " + entry.resultItem().toLowerCase(Locale.ROOT);
            if (search.isEmpty() || haystack.contains(search)) ores.add(entry);
        }
        ores.sort(Comparator.comparing(OreEntry::id));
        return ores;
    }

    private int maximumContentScroll() {
        int available = Math.max(ROW_HEIGHT, contentBottom - contentTop - 26);
        int rows;
        if (selectedOre != null) {
            rows = 15;
        } else {
            rows = switch (category) {
                case GENERAL -> 4;
                case ORES -> Math.max(1, filteredOres().size() + 1);
                case GENERATORS -> 6;
                case COMPATIBILITY -> 5;
                case MINERAL_POCKETS -> 12;
                case WORLD_GENERATION -> 3;
                case ADVANCED -> 3;
            };
        }
        return Math.max(0, rows * ROW_HEIGHT - available);
    }

    private void scrollContent(int amount) {
        contentScroll = Math.max(0, Math.min(maxContentScroll, contentScroll + amount));
        rebuildScreen();
    }

    private int nextRow() {
        int y = contentY;
        contentY += ROW_HEIGHT;
        return y;
    }

    private boolean isVisibleRow(int y) {
        return y >= contentTop && y + CONTROL_HEIGHT <= contentBottom;
    }

    private int controlX() {
        return contentX + contentWidth - CONTROL_WIDTH;
    }

    private int fieldX() {
        return contentX + Math.min(172, Math.max(112, contentWidth / 3));
    }

    private int fieldWidth() {
        return Math.max(58, contentX + contentWidth - fieldX());
    }

    private static Component toggleMessage(boolean value) {
        return Component.translatable(value ? "gui.ore_yield.toggle.on" : "gui.ore_yield.toggle.off");
    }

    private static Component pocketTypeName(MineralPocketType type) {
        return Component.translatable("gui.ore_yield.pocket." + type.configKey());
    }

    private static String pocketPrefix(MineralPocketType type) {
        return "mineral_pocket_" + type.configKey() + "_";
    }

    private static String readableOreName(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] words = path.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static String oreNamespace(String id) {
        return id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
    }

    private static String formatChance(double chance) {
        return String.format(Locale.ROOT, "%.3f%%", chance * 100.0D);
    }

    private static String formatPercent(double chance) {
        return java.math.BigDecimal.valueOf(chance * 100.0D).stripTrailingZeros().toPlainString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "") + "\"";
    }

    private static String tomlHosts(String value) {
        List<String> hosts = new ArrayList<>();
        for (String part : value.split(",")) {
            String host = part.strip().replace("\"", "");
            if (!host.isEmpty()) hosts.add(quote(host));
        }
        return "[" + String.join(", ", hosts) + "]";
    }

    private static boolean validResourceId(String value) {
        return value != null && value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }

    private static boolean validHostList(String value) {
        boolean found = false;
        for (String part : value.split(",")) {
            String host = part.strip();
            if (host.isEmpty()) continue;
            if (!validResourceId(host.startsWith("#") ? host.substring(1) : host)) return false;
            found = true;
        }
        return found;
    }

    private double number(String key) {
        return Double.parseDouble(pendingText.getOrDefault(key, "").strip().replace(',', '.'));
    }

    private int integer(String key) {
        return Integer.parseInt(pendingText.getOrDefault(key, ""));
    }

    private int integerOrDefault(String key, int fallback) {
        try {
            return integer(key);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Component helpAt(int mouseX, int mouseY) {
        if (validationMessage != null) return validationMessage;
        for (int index = hoverHelp.size() - 1; index >= 0; index--) {
            HoverHelp help = hoverHelp.get(index);
            if (help.contains(mouseX, mouseY)) return help.text();
        }
        return Component.translatable("gui.ore_yield.config.unsaved_notice").withStyle(ChatFormatting.GRAY);
    }

    private void closeToParent() {
        //? if minecraft_set_screen_and_show {
        this.minecraft.setScreenAndShow(parent);
        //?} else {
        this.minecraft.setScreen(parent);
        //?}
    }

    //? if screen_extract_render_state {
    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        //? if screen_vanilla_background {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        //?} else {
        this.extractBackground(extractor, mouseX, mouseY, partialTick);
        //?}
        extractor.centeredText(this.font, this.title, this.width / 2, 12, HEADING_TEXT_COLOR);
        extractor.centeredText(this.font, Component.translatable("gui.ore_yield.config.local_only"), this.width / 2, 21, SECONDARY_TEXT_COLOR);
        for (LayoutLabel label : labels) {
            extractor.text(this.font, label.text(), label.x(), label.y(), label.heading() ? HEADING_TEXT_COLOR : PRIMARY_TEXT_COLOR);
        }
        extractor.centeredText(this.font, helpAt(mouseX, mouseY), this.width / 2, this.height - 41,
                validationMessage == null ? SECONDARY_TEXT_COLOR : ERROR_TEXT_COLOR);
        //? if !screen_vanilla_background {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        //?}
    }
    //?} else {
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //? if screen_vanilla_background {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        //?} else {
        //? if render_background_4args {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        //?} else {
        this.renderBackground(guiGraphics);
        //?}
        //?}
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, HEADING_TEXT_COLOR);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.ore_yield.config.local_only"), this.width / 2, 21, SECONDARY_TEXT_COLOR);
        for (LayoutLabel label : labels) {
            guiGraphics.drawString(this.font, label.text(), label.x(), label.y(), label.heading() ? HEADING_TEXT_COLOR : PRIMARY_TEXT_COLOR);
        }
        guiGraphics.drawCenteredString(this.font, helpAt(mouseX, mouseY), this.width / 2, this.height - 41,
                validationMessage == null ? SECONDARY_TEXT_COLOR : ERROR_TEXT_COLOR);
        //? if !screen_vanilla_background {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        //?}
    }
    //?}

    @Override
    public void onClose() {
        closeToParent();
    }
}
