package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.BoilerSpec;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.machine.BoilerTankBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GT6 single-block Steam Boiler Tanks ({@code MultiTileEntityBoilerTank}):
 * 13 materials × {normal, strong}. The strong (dense-plate) variants carry the
 * high steam outputs. Values follow {@code Loader_MultiTileEntities} 1200-1262
 * ({@code NBT_OUTPUT_SU = tableValue * STEAM_PER_EU}, STEAM_PER_EU = 2).
 */
public final class GTBoilers {
    private static final List<RegistryObject<BoilerTankBlock>> ALL = new ArrayList<>();

    private GTBoilers() {}

    public static List<RegistryObject<BoilerTankBlock>> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** material id suffix, material, hardness, normal table value, strong table value, en, zh. */
    private record Mat(String id, GTMaterial mat, float hardness, int normal, int strong, String en, String zh) {}

    private static final Mat[] MATS = {
            new Mat("lead",           Materials.Lead,            4.0F,  16,  64, "Lead",           "铅"),
            new Mat("bismuth",        Materials.Bismuth,            4.0F,  20,  80, "Bismuth",        "铋"),
            new Mat("bronze",         Materials.Bronze,        7.0F,  24,  96, "Bronze",         "青铜"),
            new Mat("arsenic_copper", Materials.ArsenicCopper, 7.0F,  24,  96, "Arsenic Copper", "砷铜"),
            new Mat("arsenic_bronze", Materials.ArsenicBronze, 7.0F,  28, 112, "Arsenic Bronze", "砷青铜"),
            new Mat("invar",          Materials.Invar,         4.0F,  16,  64, "Invar",          "殷钢"),
            new Mat("steel",          Materials.Steel,         6.0F,  32, 128, "Steel",          "钢"),
            new Mat("chromium",       Materials.Chromium,            4.0F,  96, 384, "Chromium",       "铬"),
            new Mat("titanium",       Materials.Titanium,            9.0F, 112, 448, "Titanium",       "钛"),
            new Mat("netherite",      Materials.Netherite,     9.0F, 112, 448, "Netherite",      "下界合金"),
            new Mat("tungsten",       Materials.Tungsten,            10.0F, 128, 512, "Tungsten",       "钨"),
            new Mat("tungsten_steel", Materials.Tungstensteel,12.5F, 128, 512, "Tungsten Steel", "钨钢"),
            new Mat("ultimet",        Materials.Ultimet,      12.5F, 256,1024, "Ultimet",        "高温合金"),
    };

    private static void add(BoilerSpec spec) {
        RegistryObject<BoilerTankBlock> block = GTBlocks.BLOCKS.register(spec.id(),
                () -> new BoilerTankBlock(spec, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(spec.hardness(), spec.hardness())
                        .requiresCorrectToolForDrops()));
        ALL.add(block);
        GTBlocks.BLOCK_ITEMS.register(spec.id(), () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void registerAll() {
        for (Mat m : MATS) {
            add(new BoilerSpec("steam_boiler_" + m.id(), m.mat(), false, m.normal() * 2L, m.hardness(),
                    "Steam Boiler Tank (" + m.en() + ")", "蒸汽锅炉(" + m.zh() + ")"));
        }
        for (Mat m : MATS) {
            add(new BoilerSpec("strong_steam_boiler_" + m.id(), m.mat(), true, m.strong() * 2L, m.hardness(),
                    "Strong Steam Boiler Tank (" + m.en() + ")", "强力蒸汽锅炉(" + m.zh() + ")"));
        }
    }
}
