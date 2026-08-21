package com.oreyield.loot;

import com.oreyield.config.MineralPocketSettings;
import com.oreyield.config.MineralPocketType;
import com.oreyield.config.OreConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Independent, non-pity reward roll for the optional mineral-pocket feature. */
public final class MineralPocketRoller {
    private MineralPocketRoller() {}

    public static MineralPocketResult roll(RandomSource random) {
        if (random.nextDouble() >= OreConfig.mineralPocketChance()) return MineralPocketResult.none();

        MineralPocketType type = chooseType(random);
        if (type == null) return MineralPocketResult.none();

        List<Item> available = availableItems(type);
        if (available.isEmpty()) return MineralPocketResult.none();

        MineralPocketSettings settings = OreConfig.mineralPocketSettings(type);
        int maximumTypes = Math.min(OreConfig.mineralPocketMaxResourceTypes(), available.size());
        int minimumTypes = Math.min(Math.max(1, OreConfig.mineralPocketMinResourceTypes()), maximumTypes);
        int selectedTypes = minimumTypes + random.nextInt(maximumTypes - minimumTypes + 1);

        List<ItemStack> drops = new ArrayList<>(selectedTypes);
        for (int index = 0; index < selectedTypes; index++) {
            Item item = available.remove(random.nextInt(available.size()));
            int count = settings.minCount() + random.nextInt(settings.maxCount() - settings.minCount() + 1);
            drops.add(new ItemStack(item, count));
        }
        return new MineralPocketResult(type, drops);
    }

    private static MineralPocketType chooseType(RandomSource random) {
        int totalWeight = 0;
        for (MineralPocketType type : MineralPocketType.values()) {
            MineralPocketSettings settings = OreConfig.mineralPocketSettings(type);
            if (settings.enabled()) totalWeight += settings.weight();
        }
        if (totalWeight <= 0) return null;

        int chosen = random.nextInt(totalWeight);
        for (MineralPocketType type : MineralPocketType.values()) {
            MineralPocketSettings settings = OreConfig.mineralPocketSettings(type);
            if (!settings.enabled()) continue;
            chosen -= settings.weight();
            if (chosen < 0) return type;
        }
        return null;
    }

    private static List<Item> availableItems(MineralPocketType type) {
        List<Item> items = new ArrayList<>();
        for (String itemId : type.itemIds()) {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id == null) continue;
            //? if registry_get_optional {
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            //?} else {
            Item item = BuiltInRegistries.ITEM.get(id);
            //?}
            if (item != null && item != Items.AIR && !items.contains(item)) items.add(item);
        }
        return items;
    }
}
