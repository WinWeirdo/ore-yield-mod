package com.oreyield.fabric.mixin;

import com.oreyield.OreYieldMod;
import com.oreyield.util.ResourceLocations;
//? if advancement_icon_id {
import net.minecraft.advancements.AdvancementNode;
//?} else {
import net.minecraft.advancements.Advancement;
//?}
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {
    //? if advancement_icon_id {
    @Shadow @Final private AdvancementNode rootNode;
    //?} else {
    @Shadow @Final private Advancement advancement;
    //?}

    @ModifyVariable(
            //? if screen_extract_render_state {
            method = "extractContents",
            //?} else {
            method = "drawContents",
            //?}
            at = @At(value = "STORE"), ordinal = 0, require = 1
    )
    private ResourceLocation oreYield$useClientBackground(ResourceLocation original) {
        //? if advancement_icon_id {
        ResourceLocation rootId = rootNode.holder().id();
        //?} else {
        ResourceLocation rootId = advancement.getId();
        //?}
        return OreYieldMod.MOD_ID.equals(rootId.getNamespace())
                ? ResourceLocations.of(OreYieldMod.MOD_ID, "textures/gui/advancements/background.png")
                : original;
    }
}
