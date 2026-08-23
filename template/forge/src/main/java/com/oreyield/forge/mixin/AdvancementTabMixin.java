package com.oreyield.forge.mixin;

import com.oreyield.OreYieldMod;
import com.oreyield.util.ResourceLocations;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {
    @Shadow @Final private Advancement advancement;

    @ModifyVariable(method = "drawContents", at = @At(value = "STORE"), ordinal = 0, require = 1)
    private ResourceLocation oreYield$useClientBackground(ResourceLocation original) {
        return OreYieldMod.MOD_ID.equals(advancement.getId().getNamespace())
                ? ResourceLocations.of(OreYieldMod.MOD_ID, "textures/gui/advancements/background.png")
                : original;
    }
}
