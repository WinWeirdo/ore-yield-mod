package com.oreyield.recipe;

import com.mojang.serialization.MapCodec;
import com.oreyield.OreYieldMod;
import com.oreyield.config.OreConfig;
import com.oreyield.util.ResourceLocations;
//? if recipe_codec_api {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
//?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
//? if crafting_input_api {
import net.minecraft.world.item.crafting.CraftingInput;
//?} else {
import net.minecraft.world.inventory.CraftingContainer;
//?}
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
//? if !recipe_codec_api {
//? if !custom_recipe_serializer_api {
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
//?}
//?}
import net.minecraft.world.level.Level;
//? if !crafting_input_api {
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.RegistryAccess;
//?} else if !recipe_codec_api {
import net.minecraft.core.HolderLookup;
//?}

/** A config-driven 3x3 recipe, evaluated whenever its crafting grid changes. */
public final class StoneGeneratorRecipe extends CustomRecipe {
    //? if recipe_codec_api {
    public static final StoneGeneratorRecipe INSTANCE = new StoneGeneratorRecipe();
    public static final MapCodec<StoneGeneratorRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, StoneGeneratorRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<StoneGeneratorRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public StoneGeneratorRecipe() {
        super();
    }
    //?} else if custom_recipe_serializer_api {
    public static final RecipeSerializer<StoneGeneratorRecipe> SERIALIZER = new CustomRecipe.Serializer<>(StoneGeneratorRecipe::new);

    public StoneGeneratorRecipe(CraftingBookCategory category) {
        super(category);
    }
    //?} else if crafting_input_api {
    public static final RecipeSerializer<StoneGeneratorRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(StoneGeneratorRecipe::new);

    public StoneGeneratorRecipe(CraftingBookCategory category) {
        super(category);
    }
    //?} else {
    public static final RecipeSerializer<StoneGeneratorRecipe> SERIALIZER = new SimpleCraftingRecipeSerializer<>(StoneGeneratorRecipe::new);

    public StoneGeneratorRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }
    //?}

    @Override
    //? if crafting_input_api {
    public boolean matches(CraftingInput input, Level level) {
        if (input.size() != 9) return false;
        return matchesGrid(input::getItem);
    }

    @Override
    //? if recipe_codec_api {
    public ItemStack assemble(CraftingInput input) {
    //?} else {
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registryAccess) {
    //?}
        return generatorStack();
    }
    //?} else {
    public boolean matches(CraftingContainer input, Level level) {
        if (input.getContainerSize() != 9) return false;
        return matchesGrid(input::getItem);
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registryAccess) {
        return generatorStack();
    }
    //?}

    //? if legacy_crafting_dimensions {
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }
    //?}

    @Override
    public RecipeSerializer<StoneGeneratorRecipe> getSerializer() {
        return SERIALIZER;
    }

    private static boolean matchesGrid(java.util.function.IntFunction<ItemStack> stackAt) {
        Item surrounding = configuredItem(OreConfig.stoneGeneratorSurroundingItem());
        Item center = configuredItem(OreConfig.stoneGeneratorCenterItem());
        if (surrounding == Items.AIR || center == Items.AIR) return false;
        for (int slot = 0; slot < 9; slot++) {
            Item expected = slot == 4 ? center : surrounding;
            if (!stackAt.apply(slot).is(expected)) return false;
        }
        return true;
    }

    private static ItemStack generatorStack() {
        Item item = configuredItem(OreYieldMod.MOD_ID + ":stone_generator");
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Item configuredItem(String rawId) {
        var id = ResourceLocations.tryParse(rawId);
        if (id == null) return Items.AIR;
        //? if registry_get_optional {
        return BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        //?} else {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null ? Items.AIR : item;
        //?}
    }
}
