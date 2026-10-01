package com.gregtech.gregtech.loaders.a;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.MaterialItemDefinitions;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.item.CoinItem;
import com.gregtech.gregtech.item.MaterialItem;
import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTTechnological;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;


/**
 * Registers one GT6 material item per valid (prefix, material) pair.
 * The shared definition stream retains the original explicit prefix order and material filtering.
 */
public record Loader_Items() implements IGTLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void run() {
        int total = 0;
        for (MaterialItemDefinitions.Definition definition : MaterialItemDefinitions.all()) {
            MaterialPrefix prefix = definition.prefix();
            GTMaterial material = definition.material();
            if (definition.hasIdSuffix()) {
                LOGGER.warn("Duplicate item id '{}' for material {} (id={}); using '{}'",
                        definition.baseItemId(), material.getName(), material.getId(), definition.itemId());
            }
            RegistryObject<Item> registered = GTItems.ITEMS.register(definition.itemId(), () ->
                    prefix == MaterialPrefix.coin
                            ? new CoinItem(new Item.Properties().stacksTo(64), material)
                            : new MaterialItem(new Item.Properties().stacksTo(64), prefix, material));
            GTItems.bind(prefix, material, registered);
            total++;
        }

        LOGGER.info("Queued {} material items for registration", total);

        GTTechnological.registerAll();
        LOGGER.info("Queued {} technological items for registration", GTTechnological.all().size());

        com.gregtech.gregtech.registry.GTMultiItems.registerAll();
        LOGGER.info("Queued {} multi-items (tools/bottles/food/bumblebees) for registration",
                com.gregtech.gregtech.registry.GTMultiItems.all().size());

        // §108: GT6's food items are all inside the multi-item table above; this registry only holds
        // the handful that were genuinely missing (today none - see GTFoodItemsGen.GAP).
        com.gregtech.gregtech.registry.GTFoodItems.registerAll();
        LOGGER.info("Queued {} GT6 food items the multi-item table was missing",
                com.gregtech.gregtech.registry.GTFoodItems.registered());

        com.gregtech.gregtech.registry.GTFuelRods.registerAll();
        LOGGER.info("Queued {} fuel rod items for registration",
                com.gregtech.gregtech.registry.GTFuelRods.ALL.size());
    }

}
