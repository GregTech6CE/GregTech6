package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.PumpSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.energy.PumpBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** GT6 rotational pumps — RU-powered fluid drainage, 4 tiers. */
public final class GTPumps {
    private static final List<RegistryObject<PumpBlock>> ALL = new ArrayList<>();

    private GTPumps() {}

    public static List<RegistryObject<PumpBlock>> all() { return Collections.unmodifiableList(ALL); }

    public static void registerAll() {
        record Tier(String name, GTMaterial mat, long speed) {}
        Tier[] tiers = {
                new Tier("bronze", Materials.Bronze, 32),
                new Tier("steel", Materials.Steel, 128),
                new Tier("titanium", Materials.Titanium, 512),
                new Tier("tungstensteel", Materials.Tungstensteel, 2048),
        };

        for (int i = 0; i < tiers.length; i++) {
            Tier t = tiers[i];
            String id = "rotational_pump_" + t.name();
            PumpSpec spec = new PumpSpec(id, t.mat(), t.speed(), i + 1);
            RegistryObject<PumpBlock> block = GTBlocks.BLOCKS.register(id,
                    () -> new PumpBlock(spec, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(4.0f, 6.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()));
            ALL.add(block);
            GTBlocks.BLOCK_ITEMS.register(id,
                    () -> new BlockItem(block.get(), new Item.Properties()));
        }
    }

    public static void bootstrap() {
        if (ALL.isEmpty()) throw new IllegalStateException("GTPumps failed to initialize");
    }
}
