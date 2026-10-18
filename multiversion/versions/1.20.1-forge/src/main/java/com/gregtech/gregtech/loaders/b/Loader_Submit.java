package com.gregtech.gregtech.loaders.b;

import com.gregtech.gregtech.loaders.IGTLoader;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.registry.GTMenuTypes;
import com.gregtech.gregtech.registry.GTSounds;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraftforge.eventbus.api.IEventBus;

/** Final Phase B step — submits all shared DeferredRegisters after every bootstrapper has queued. */
public record Loader_Submit(IEventBus bus) implements IGTLoader {
    @Override
    public void run() {
        GTItems.ITEMS.register(bus);
        GTTechnological.ITEMS.register(bus);
        com.gregtech.gregtech.registry.GTMultiItems.register(bus);
        // §108: the few GT6 food items the port was actually missing (today: none - the 266 food
        // items were all registered already, only their numbers were missing) submit here.
        com.gregtech.gregtech.registry.GTFoodItems.register(bus);
        GTBlocks.BLOCKS.register(bus);
        GTBlocks.BLOCK_ITEMS.register(bus);
        // GTIconSetBlocks uses GTBlocks.BLOCKS / GTBlocks.BLOCK_ITEMS directly
        GTSounds.REGISTRY.register(bus);
        GTMenuTypes.MENU_TYPES.register(bus);
    }
}
