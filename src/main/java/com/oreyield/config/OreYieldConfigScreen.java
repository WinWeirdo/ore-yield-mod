package com.oreyield.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OreYieldConfigScreen extends Screen {
    private final Screen parent;
    private final boolean[] pendingValues = new boolean[5];
    private Button[] toggleButtons = new Button[5];
    private static final String[] KEYS = {
            "remove_vanilla_ore_generation",
            "enable_mod_compat",
            "enable_mod_compat_2",
            "mod_compat_2_ores_in_end",
            "enable_vanilla_end_ores"
    };
    private static final String[] LABELS = {
            "Remove Vanilla Ore Generation",
            "Enable Mod Compat (Experimental)",
            "Enable Mod Compat 2 (Curated)",
            "Mod Compat 2 Ores in End",
            "Enable Vanilla End Ores"
    };
    private static final String[] DESCRIPTIONS = {
            "When true, vanilla ore placed-features are removed from terrain generation.",
            "Auto-detect modded ore blocks at startup. Requires restart for full effect.",
            "Curated modded ores (iceandfire, simpleores, better_tools, tconstruct, netherrocks).",
            "When true, mod_compat_2 ores also drop in the End dimension.",
            "When true, vanilla End ore drops (coal, iron, copper, gold, etc.) are active."
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

        int centerX = this.width / 2;
        int startY = 40;
        int rowHeight = 30;

        for (int i = 0; i < 5; i++) {
            final int index = i;
            int y = startY + i * rowHeight;
            toggleButtons[i] = Button.builder(
                    Component.literal(getToggleText(pendingValues[i])),
                    btn -> {
                        pendingValues[index] = !pendingValues[index];
                        btn.setMessage(Component.literal(getToggleText(pendingValues[index])));
                    }
            ).bounds(centerX + 100, y, 80, 20).build();
            this.addRenderableWidget(toggleButtons[i]);
        }

        int buttonY = startY + 5 * rowHeight + 10;
        this.addRenderableWidget(Button.builder(
                Component.literal("Reload from File"),
                btn -> {
                    OreConfig.reloadFromFile();
                    pendingValues[0] = OreConfig.shouldRemoveVanillaOreGeneration();
                    pendingValues[1] = OreConfig.isModCompatEnabled();
                    pendingValues[2] = OreConfig.isModCompat2Enabled();
                    pendingValues[3] = OreConfig.isModCompat2OresInEnd();
                    pendingValues[4] = OreConfig.isVanillaEndOresEnabled();
                    for (int j = 0; j < 5; j++) {
                        toggleButtons[j].setMessage(Component.literal(getToggleText(pendingValues[j])));
                    }
                }
        ).bounds(centerX - 100, buttonY, 95, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Save & Close"),
                btn -> {
                    for (int i = 0; i < 5; i++) {
                        OreConfig.setValue(KEYS[i], pendingValues[i]);
                    }
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
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 15, 0xFFFFFF);

        int startY = 40;
        int rowHeight = 30;

        for (int i = 0; i < 5; i++) {
            int y = startY + i * rowHeight + 6;
            guiGraphics.drawString(this.font, Component.literal(LABELS[i]).withStyle(ChatFormatting.WHITE), centerX - 150, y, 0xFFFFFF);
            guiGraphics.drawString(this.font, Component.literal(DESCRIPTIONS[i]).withStyle(ChatFormatting.GRAY), centerX - 150, y + 12, 0xAAAAAA);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
