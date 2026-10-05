package com.gregtech.gregtech.registry;
import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.content.plant.BerryBushCatalog;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

/** Independent food, cotton and material berry identities, plus the unplanted bush. */
public final class GTBushes {
    private GTBushes() {}
    private static final Map<String,RegistryObject<BushBlock>> VARIANTS=new LinkedHashMap<>();
    public static final RegistryObject<BushBlock> BUSH=register("bush", "");
    static {
        for(var type:BerryBushCatalog.worldgenTypes()) register(BerryBushCatalog.blockPath(type.id()),type.id());
        for(var type:com.gregtech.gregtech.content.plant.MaterialBerryBushCatalog.variants()) register(type.blockPath(),type.berryItemId());
    }
    private static RegistryObject<BushBlock> register(String path,String berry) {
        var block=GTBlocks.BLOCKS.register(path,()->new BushBlock(berry,BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT).strength(.2f).sound(SoundType.GRASS).noOcclusion().instabreak()));
        VARIANTS.put(path,block);return block;
    }
    public static BushBlock byBerry(String berry) {
        var entry=VARIANTS.get(BerryBushCatalog.blockPath(berry));return entry==null?null:entry.get();
    }
    public static Block[] allBlocks() { return VARIANTS.values().stream().map(e->(Block)e.get()).toArray(Block[]::new); }
    public static void registerAll() { VARIANTS.forEach((path,block)->GTBlocks.BLOCK_ITEMS.register(path,()->new com.gregtech.gregtech.item.BushBlockItem(block.get(),new Item.Properties()))); }
}
