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

public final class OreYieldConfigScreen extends Screen {
    // Modern GUI draw calls require an explicit alpha channel; older GuiGraphics
    // versions accept RGB colors without it.
    //? if screen_extract_render_state {
    private static final int PRIMARY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0xFFAAAAAA;
    //?} else if gui_graphics_argb {
    private static final int PRIMARY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0xFFAAAAAA;
    //?} else {
    private static final int PRIMARY_TEXT_COLOR = 0xFFFFFF;
    private static final int SECONDARY_TEXT_COLOR = 0xAAAAAA;
    //?}
    private static final int SETTING_COUNT = 7;
    private static final int SETTING_START_Y = 35;
    private static final int SETTING_ROW_HEIGHT = 19;
    private static final int SETTING_CONTROL_WIDTH = 60;
    private static final int SETTING_CONTROL_HEIGHT = 18;
    private static final int MULTIPLIER_Y = SETTING_START_Y + SETTING_COUNT * SETTING_ROW_HEIGHT + 6;
    private static final int HELP_Y = MULTIPLIER_Y + 25;
    private static final int ACTION_Y = HELP_Y + 14;
    private static final int ACTION_WIDTH = 94;
    private static final int ACTION_HEIGHT = 20;
    private final Screen parent;
    private final boolean[] pendingValues = new boolean[SETTING_COUNT];
    private Button[] toggleButtons = new Button[SETTING_COUNT];
    private EditBox multiplierField;
    private static final String[] KEYS = {
            "remove_vanilla_ore_generation",
            "enable_mod_compat",
            "enable_mod_compat_2",
            "mod_compat_2_ores_in_end",
            "enable_vanilla_end_ores",
            "bad_luck_eliminator",
            "auto_detect_dimensions"
    };
    private static final String[] LABELS = {
            "Remove Vanilla Ore Generation",
            "Enable Mod Compat (Experimental)",
            "Enable Mod Compat 2 (Curated)",
            "Mod Compat 2 Ores in End",
            "Enable Vanilla End Ores",
            "Bad Luck Eliminator",
            "Auto Detect Dimensions"
    };
    private static final String[] DESCRIPTIONS = {
            "Removes ore features from newly generated terrain.",
            "Scans compatible modded ores after a restart.",
            "Enables built-in rules for supported mods.",
            "Allows curated ore drops in the End.",
            "Allows vanilla ore drops in the End.",
            "Forces drops after repeated eligible misses.",
            "Adds detected modded dimensions after a restart."
    };

    public OreYieldConfigScreen(Screen parent) {
        super(Component.literal("Ore Yield Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pendingValues[0] = OreConfig.shouldRemoveVanillaOreGeneration();
        pendingValues[1] = OreConfig.isModCompatEnabled();
        pendingValues[2] = OreConfig.isModCompat2Enabled();
        pendingValues[3] = OreConfig.isModCompat2OresInEnd();
        pendingValues[4] = OreConfig.isVanillaEndOresEnabled();
        pendingValues[5] = OreConfig.isBadLuckEliminatorEnabled();
        pendingValues[6] = OreConfig.isAutoDetectDimensionsEnabled();

        int controlX = settingControlX();

        for (int i = 0; i < SETTING_COUNT; i++) {
            final int index = i;
            int y = settingY(i);
            toggleButtons[i] = Button.builder(
                    Component.literal(getToggleText(pendingValues[i])),
                    btn -> {
                        pendingValues[index] = !pendingValues[index];
                        btn.setMessage(Component.literal(getToggleText(pendingValues[index])));
                    }
            ).bounds(controlX, y, SETTING_CONTROL_WIDTH, SETTING_CONTROL_HEIGHT).build();
            this.addRenderableWidget(toggleButtons[i]);
        }

        multiplierField = new EditBox(this.font, controlX, MULTIPLIER_Y, SETTING_CONTROL_WIDTH, SETTING_CONTROL_HEIGHT, Component.literal("Bad Luck Multiplier"));
        multiplierField.setMaxLength(8);
        multiplierField.setValue(String.valueOf(OreConfig.badLuckMultiplier()));
        this.addRenderableWidget(multiplierField);

        int actionStartX = (this.width - (ACTION_WIDTH * 3 + 18)) / 2;
        this.addRenderableWidget(Button.builder(
                Component.literal("Reload from File"),
                btn -> {
                    OreConfig.reloadFromFile();
                    pendingValues[0] = OreConfig.shouldRemoveVanillaOreGeneration();
                    pendingValues[1] = OreConfig.isModCompatEnabled();
                    pendingValues[2] = OreConfig.isModCompat2Enabled();
                    pendingValues[3] = OreConfig.isModCompat2OresInEnd();
                    pendingValues[4] = OreConfig.isVanillaEndOresEnabled();
                    pendingValues[5] = OreConfig.isBadLuckEliminatorEnabled();
                    pendingValues[6] = OreConfig.isAutoDetectDimensionsEnabled();
                    multiplierField.setValue(String.valueOf(OreConfig.badLuckMultiplier()));
                    for (int j = 0; j < SETTING_COUNT; j++) {
                        toggleButtons[j].setMessage(Component.literal(getToggleText(pendingValues[j])));
                    }
                }
        ).bounds(actionStartX, ACTION_Y, ACTION_WIDTH, ACTION_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Save & Close"),
                btn -> {
                    for (int i = 0; i < SETTING_COUNT; i++) {
                        OreConfig.setValue(KEYS[i], pendingValues[i]);
                    }
                    double multiplier = OreConfig.badLuckMultiplier();
                    try {
                        double parsedMultiplier = Double.parseDouble(multiplierField.getValue().replace(',', '.'));
                        if (Double.isFinite(parsedMultiplier)) {
                            multiplier = Math.max(1.0D, parsedMultiplier);
                        }
                    } catch (NumberFormatException ignored) {
                        // invalid input keeps the previous value
                    }
                    OreConfig.setValue("bad_luck_multiplier", multiplier);
                    OreConfig.saveAndRebuild();
                    //? if minecraft_set_screen_and_show {
                    this.minecraft.setScreenAndShow(parent);
                    //?} else {
                    this.minecraft.setScreen(parent);
                    //?}
                }
        ).bounds(actionStartX + ACTION_WIDTH + 9, ACTION_Y, ACTION_WIDTH, ACTION_HEIGHT).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                //? if minecraft_set_screen_and_show {
                btn -> this.minecraft.setScreenAndShow(parent)
                //?} else {
                btn -> this.minecraft.setScreen(parent)
                //?}
        ).bounds(actionStartX + (ACTION_WIDTH + 9) * 2, ACTION_Y, ACTION_WIDTH, ACTION_HEIGHT).build());
    }

    private int settingControlX() {
        return this.width - SETTING_CONTROL_WIDTH - 10;
    }

    private static int settingY(int index) {
        return SETTING_START_Y + index * SETTING_ROW_HEIGHT;
    }

    private int hoveredSetting(int mouseY) {
        for (int i = 0; i < SETTING_COUNT; i++) {
            int y = settingY(i);
            if (mouseY >= y && mouseY < y + SETTING_CONTROL_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    private Component helpText(int mouseY) {
        int setting = hoveredSetting(mouseY);
        return Component.literal(setting >= 0 ? DESCRIPTIONS[setting] : "Hover a setting for details.").withStyle(ChatFormatting.GRAY);
    }

    private static String getToggleText(boolean value) {
        return value ? "ON" : "OFF";
    }

    @Override
    //? if screen_extract_render_state {
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        //? if screen_vanilla_background {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        //?} else {
        this.extractBackground(extractor, mouseX, mouseY, partialTick);
        //?}
        int centerX = this.width / 2;
        extractor.centeredText(this.font, this.title, centerX, 12, PRIMARY_TEXT_COLOR);

        extractor.centeredText(this.font, Component.literal("This screen does not change multiplayer server settings.").withStyle(ChatFormatting.GRAY), centerX, 20, SECONDARY_TEXT_COLOR);
        for (int i = 0; i < SETTING_COUNT; i++) {
            int y = settingY(i) + 5;
            extractor.text(this.font, Component.literal(LABELS[i]).withStyle(ChatFormatting.WHITE), 10, y, PRIMARY_TEXT_COLOR);
        }

        extractor.text(this.font, Component.literal("Bad Luck Multiplier (x)").withStyle(ChatFormatting.WHITE), 10, MULTIPLIER_Y + 5, PRIMARY_TEXT_COLOR);
        extractor.centeredText(this.font, helpText(mouseY), centerX, HELP_Y, SECONDARY_TEXT_COLOR);

        //? if !screen_vanilla_background {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        //?}
    }
    //?} else {
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
        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 12, PRIMARY_TEXT_COLOR);

        guiGraphics.drawCenteredString(this.font, Component.literal("This screen does not change multiplayer server settings.").withStyle(ChatFormatting.GRAY), centerX, 20, SECONDARY_TEXT_COLOR);
        for (int i = 0; i < SETTING_COUNT; i++) {
            int y = settingY(i) + 5;
            guiGraphics.drawString(this.font, Component.literal(LABELS[i]).withStyle(ChatFormatting.WHITE), 10, y, PRIMARY_TEXT_COLOR);
        }

        guiGraphics.drawString(this.font, Component.literal("Bad Luck Multiplier (x)").withStyle(ChatFormatting.WHITE), 10, MULTIPLIER_Y + 5, PRIMARY_TEXT_COLOR);
        guiGraphics.drawCenteredString(this.font, helpText(mouseY), centerX, HELP_Y, SECONDARY_TEXT_COLOR);

        //? if !screen_vanilla_background {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        //?}
    }
    //?}

    @Override
    public void onClose() {
        //? if minecraft_set_screen_and_show {
        this.minecraft.setScreenAndShow(parent);
        //?} else {
        this.minecraft.setScreen(parent);
        //?}
    }
}
