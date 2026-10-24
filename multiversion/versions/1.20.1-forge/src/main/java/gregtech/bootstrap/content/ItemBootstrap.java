package gregtech.bootstrap.content;

import gregtech.internal.material.GTMaterial;
import gregtech.internal.material.MaterialItemDefinitions;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.item.CoinItem;
import gregtech.content.item.MaterialItem;
import gregtech.bootstrap.InitializationStep;
import gregtech.content.registry.GTItems;
import gregtech.content.registry.GTTechnological;
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
                            ? new gregtech.content.item.MaterialArrowItem(new Item.Properties().stacksTo(64), prefix, material)
                            : gregtech.content.item.EnergyCrystalItem.createOrMaterial(prefix, material));
            GTItems.bind(prefix, material, registered);
            total++;
        }

        LOGGER.info("Queued {} material items for registration", total);

        gregtech.content.registry.GTColoredBooks.registerAll();
        GTTechnological.registerAll();
        LOGGER.info("Queued {} technological items for registration", GTTechnological.all().size());

        gregtech.content.registry.GTMultiItems.registerAll();
        LOGGER.info("Queued {} multi-items (tools/bottles/food/bumblebees) for registration",
                gregtech.content.registry.GTMultiItems.all().size());

        // §108: GT6's food items are all inside the multi-item table above; this registry only holds
        // the handful that were genuinely missing (today none - see GTFoodItemsGen.GAP).
        gregtech.content.registry.GTFoodItems.registerAll();
        LOGGER.info("Queued {} GT6 food items the multi-item table was missing",
                gregtech.content.registry.GTFoodItems.registered());

        gregtech.content.registry.GTFuelRods.registerAll();
        LOGGER.info("Queued {} fuel rod items for registration",
                gregtech.content.registry.GTFuelRods.ALL.size());
    }

}
