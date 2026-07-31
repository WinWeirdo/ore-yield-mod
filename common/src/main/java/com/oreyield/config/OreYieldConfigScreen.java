package com.oreyield.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OreYieldConfigScreen extends Screen {
    private final Screen parent;
    private final boolean[] pendingValues = new boolean[6];
    private Button[] toggleButtons = new Button[6];
    private EditBox multiplierField;
    private static final String[] KEYS = {
            "remove_vanilla_ore_generation",
            "enable_mod_compat",
            "enable_mod_compat_2",
            "mod_compat_2_ores_in_end",
            "enable_vanilla_end_ores",
            "bad_luck_eliminator"
    };
    private static final String[] LABELS = {
            "Remove Vanilla Ore Generation",
            "Enable Mod Compat (Experimental)",
            "Enable Mod Compat 2 (Curated)",
            "Mod Compat 2 Ores in End",
            "Enable Vanilla End Ores",
            "Bad Luck Eliminator"
    };
    private static final String[] DESCRIPTIONS = {
            "When true, vanilla ore placed-features are removed from terrain generation.",
            "Auto-detect modded ore blocks at startup. Requires restart for full effect.",
            "Curated modded ores (iceandfire, simpleores, better_tools, tconstruct, netherrocks).",
            "When true, mod_compat_2 ores also drop in the End dimension.",
            "When true, vanilla End ore drops (coal, iron, copper, gold, etc.) are active.",
            "Guarantees an ore drop after a bad-luck streak (window = multiplier / chance)."
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

        int centerX = this.width / 2;
        int startY = 32;
        int rowHeight = 27;

        for (int i = 0; i < 6; i++) {
            final int index = i;
            int y = startY + i * rowHeight;
            toggleButtons[i] = Button.builder(
                    Component.literal(getToggleText(pendingValues[i])),
                    btn -> {
                        pendingValues[index] = !pendingValues[index];
                        btn.setMessage(Component.literal(getToggleText(pendingValues[index])));
                    }
            ).bounds(centerX + 110, y, 60, 20).build();
            this.addRenderableWidget(toggleButtons[i]);
        }

        int multiplierY = startY + 6 * rowHeight + 2;
        multiplierField = new EditBox(this.font, centerX + 110, multiplierY, 60, 20, Component.literal("Bad Luck Multiplier"));
        multiplierField.setMaxLength(8);
        multiplierField.setValue(String.valueOf(OreConfig.badLuckMultiplier()));
        this.addRenderableWidget(multiplierField);

        int buttonY = multiplierY + 30;
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
                    multiplierField.setValue(String.valueOf(OreConfig.badLuckMultiplier()));
                    for (int j = 0; j < 6; j++) {
                        toggleButtons[j].setMessage(Component.literal(getToggleText(pendingValues[j])));
                    }
                }
        ).bounds(centerX - 100, buttonY, 95, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Save & Close"),
                btn -> {
                    for (int i = 0; i < 6; i++) {
                        OreConfig.setValue(KEYS[i], pendingValues[i]);
                    }
                    double multiplier = OreConfig.badLuckMultiplier();
                    try {
                        multiplier = Math.max(1.0D, Double.parseDouble(multiplierField.getValue().replace(',', '.')));
                    } catch (NumberFormatException ignored) {
                        // invalid input keeps the previous value
                    }
                    OreConfig.setValue("bad_luck_multiplier", multiplier);
                    OreConfig.saveAndRebuild();
                    this.minecraft.setScreen(parent);
                }
        ).bounds(centerX - 100, buttonY + 25, 95, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Done"),
                btn -> this.minecraft.setScreen(parent)
        ).bounds(centerX + 5, buttonY + 25, 95, 20).build());
    }

    private static String getToggleText(boolean value) {
        return value ? "ON" : "OFF";
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 12, 0xFFFFFF);

        int startY = 32;
        int rowHeight = 27;

        for (int i = 0; i < 6; i++) {
            int y = startY + i * rowHeight + 4;
            guiGraphics.drawString(this.font, Component.literal(LABELS[i]).withStyle(ChatFormatting.WHITE), centerX - 160, y, 0xFFFFFF);
            guiGraphics.drawString(this.font, Component.literal(DESCRIPTIONS[i]).withStyle(ChatFormatting.GRAY), centerX - 160, y + 11, 0xAAAAAA);
        }

        int multiplierY = startY + 6 * rowHeight + 6;
        guiGraphics.drawString(this.font, Component.literal("Bad Luck Multiplier (x)").withStyle(ChatFormatting.WHITE), centerX - 160, multiplierY, 0xFFFFFF);
        guiGraphics.drawString(this.font, Component.literal("Guarantee window: ceil(multiplier / chance) eligible blocks.").withStyle(ChatFormatting.GRAY), centerX - 160, multiplierY + 11, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
