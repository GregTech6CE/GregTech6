package gregtech.bootstrap.content;

import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.MaterialItemDefinitions;
import gregtech.data.MaterialPrefix;
import gregtech.item.CoinItem;
import gregtech.item.MaterialItem;
import gregtech.bootstrap.InitializationStep;
import gregtech.registry.GTItems;
import gregtech.registry.GTTechnological;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;


/**
 * Registers one GT6 material item per valid (prefix, material) pair.
 * The shared definition stream retains the original explicit prefix order and material filtering.
 */
public record ItemBootstrap() implements InitializationStep {
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
            RegistryObject<Item> registered = GTItems.items(definition.namespace()).register(definition.itemId(), () ->
                    prefix == MaterialPrefix.coin
                            ? new CoinItem(new Item.Properties().stacksTo(64), material)
                            : gregtech.content.recipe.MaterialArrowRules.isArrow(prefix, material)
                            ? new gregtech.item.MaterialArrowItem(new Item.Properties().stacksTo(64), prefix, material)
                            : gregtech.item.EnergyCrystalItem.createOrMaterial(prefix, material));
            GTItems.bind(prefix, material, registered);
            total++;
        }

        LOGGER.info("Queued {} material items for registration", total);

        gregtech.registry.GTColoredBooks.registerAll();
        GTTechnological.registerAll();
        LOGGER.info("Queued {} technological items for registration", GTTechnological.all().size());

        gregtech.registry.GTMultiItems.registerAll();
        LOGGER.info("Queued {} multi-items (tools/bottles/food/bumblebees) for registration",
                gregtech.registry.GTMultiItems.all().size());

        // §108: GT6's food items are all inside the multi-item table above; this registry only holds
        // the handful that were genuinely missing (today none - see GTFoodItemsGen.GAP).
        gregtech.registry.GTFoodItems.registerAll();
        LOGGER.info("Queued {} GT6 food items the multi-item table was missing",
                gregtech.registry.GTFoodItems.registered());

        gregtech.registry.GTFuelRods.registerAll();
        LOGGER.info("Queued {} fuel rod items for registration",
                gregtech.registry.GTFuelRods.ALL.size());
    }

}
