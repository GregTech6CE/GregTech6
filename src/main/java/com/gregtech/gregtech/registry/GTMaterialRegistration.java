package com.gregtech.gregtech.registry;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.MachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.ItemMaterialRegistry;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/** Registers GT block items in {@link ItemMaterialRegistry} so F3+H advanced tooltips show material composition. */
public final class GTMaterialRegistration {
    private GTMaterialRegistration() {}

    /** Call from common setup after all blocks are registered. */
    public static void registerAll() {
        GregTech.LOGGER.info("[{}] Registering GT block item materials for F3+H tooltips", GregTech.NAMESPACE);

        int count = 0;
        // Material items carry their identity on the Item instance. Fuel/ash and recovery
        // consumers use the same composition registry as block items and vanilla forms.
        for (var holder : GTItems.allEntries()) {
            if (holder.isPresent() && holder.get() instanceof com.gregtech.gregtech.item.MaterialItem item) {
                ItemMaterialRegistry.register(item, item.getPrefix(), item.getMaterial());
                count++;
            }
        }
        // The original GT6 ore unifier treats this placeable block as one
        // plate of WoodTreated. Keep recipes, material tags and recycling aligned.
        ItemMaterialRegistry.register(GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.plate,
                        com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated).getItem(),
                com.gregtech.gregtech.data.MaterialPrefix.plate,
                com.gregtech.gregtech.content.material.generated.WoodMaterials.WoodTreated);
        count++;
        for (var entry : GTSignalWires.all()) {
            ItemMaterialRegistry.register(entry.get().asItem(), null, entry.get().material(), GTValues.U / 2);
            count++;
        }

        // Wires & cables — use exact materialAmount from WireSpec
        for (RegistryObject<ElectricWireBlock> entry : GTWires.all()) {
            if (!entry.isPresent()) continue;
            ElectricWireBlock block = entry.get();
            if (block.spec() == null || block.spec().material() == null) continue;
            count++;
            ItemMaterialRegistry.register(block.asItem(), null,
                    block.spec().material(), block.spec().materialAmount());
        }

        // Fluid pipes
        for (RegistryObject<FluidPipeBlock> entry : GTFluidPipes.all()) {
            if (!entry.isPresent()) continue;
            FluidPipeBlock block = entry.get();
            if (block.spec() == null || block.spec().material() == null) continue;
            count++;
            ItemMaterialRegistry.register(block.asItem(), null, block.spec().material(), GTValues.U);
        }

        // Item pipes
        for (RegistryObject<ItemPipeBlock> entry : GTItemPipes.all()) {
            if (!entry.isPresent()) continue;
            ItemPipeBlock block = entry.get();
            if (block.spec() == null || block.spec().material() == null) continue;
            count++;
            ItemMaterialRegistry.register(block.asItem(), null, block.spec().material(), GTValues.U);
        }

        // Tanks
        for (RegistryObject<TankBlock> entry : GTTanks.all()) {
            if (!entry.isPresent()) continue;
            TankBlock block = entry.get();
            count++;
            ItemMaterialRegistry.register(block.asItem(), null, block.spec().material(), GTValues.U);
        }

        // Hoppers
        count += registerMachineList(MachineRegistry.hoppers(), GTValues.U);
        count += registerMachineList(MachineRegistry.queueHoppers(), GTValues.U);

        // Burning boxes (MachineSpec doesn't store GTMaterial — look up by materialName)
        for (RegistryObject<SolidBurningBoxBlock> entry : MachineRegistry.solidBurningBoxes()) {
            if (!entry.isPresent()) continue;
            SolidBurningBoxBlock block = entry.get();
            MachineSpec spec = block.spec();
            GTMaterial mat = GTMaterialRegistry.get(spec.materialName());
            if (mat.getId() <= 0) continue;
            count++;
            ItemMaterialRegistry.register(block.asItem(), null, mat, GTValues.U);
        }

        // Smelting crucibles
        for (RegistryObject<SmeltingCrucibleBlock> entry : MachineRegistry.smeltingCrucibles()) {
            if (!entry.isPresent()) continue;
            SmeltingCrucibleBlock block = entry.get();
            CrucibleSpec spec = block.spec();
            if (spec == null || spec.material() == null) continue;
            count++;
            ItemMaterialRegistry.register(block.asItem(), null, spec.material(), spec.hullMaterialUnits());
        }

        // Smeltery companion blocks
        count += registerSmelteryList(MachineRegistry.molds(), CrucibleSpec.MOLD_HULL_UNITS);
        count += registerSmelteryList(MachineRegistry.moldBasins(), CrucibleSpec.BASIN_HULL_UNITS);
        count += registerSmelteryList(MachineRegistry.crucibleCrossings(), CrucibleSpec.CROSSING_HULL_UNITS);
        count += registerSmelteryList(MachineRegistry.crucibleFaucets(), CrucibleSpec.FAUCET_HULL_UNITS);

        com.gregtech.gregtech.content.recipe.DiggableRecipes.registerMaterials();
        count += 8; // Five clay blocks, turf, and two legacy ids; five clay balls are items.

        // GT6 BlockSands metas 0..2 are OP.blockDust forms: one placed sand is nine dust units.
        // Keep the original sand texture block distinct from the generic MaterialBlock model.
        for (RegistryObject<Block> entry : GTIconSetBlocks.all()) {
            if (!entry.isPresent() || !(entry.get() instanceof com.gregtech.gregtech.block.BlackSandBlock sand))
                continue;
            ItemMaterialRegistry.register(sand.asItem(), null, sand.material(), GTValues.U * 9);
            count++;
        }

        // GT6 BlockCrystalOres / BlockRockOres register their block items as OP.oreDense.
        // This is a 4U ore form; no separate material item or replacement model is generated.
        for (RegistryObject<Block> entry : GTIconSetBlocks.all()) {
            if (!entry.isPresent() || !(entry.get() instanceof com.gregtech.gregtech.block.DenseOreBlock denseOre))
                continue;
            Block block = entry.get();
            GTMaterial oreMaterial = denseOre.material();
            if (oreMaterial == null || !oreMaterial.isValid()) continue;
            ItemMaterialRegistry.register(block.asItem(),
                    com.gregtech.gregtech.data.MaterialPrefixes.oreDense.getDelegate(), oreMaterial);
            count++;
        }

        // GT6 BlockVanillaOresA metas 0..15 carry OP.oreVanillastone, an ordinary 2U ore.
        for (RegistryObject<Block> entry : GTIconSetBlocks.all()) {
            if (!entry.isPresent() || !(entry.get() instanceof com.gregtech.gregtech.block.VanillaOreBlock ore))
                continue;
            GTMaterial oreMaterial = ore.material();
            if (oreMaterial == null || !oreMaterial.isValid()) continue;
            ItemMaterialRegistry.register(ore.asItem(),
                    com.gregtech.gregtech.data.MaterialPrefixes.oreVanillastone.getDelegate(), oreMaterial);
            count++;
        }

        // GT6's ordinary and broken host-stone ores are both OP.oreVanillastone (2U).
        // The port shares one block ID per material and marks the broken form in BlockStateTag.
        for (RegistryObject<Block> entry : GTBlocks.allEntries()) {
            if (!entry.isPresent() || !(entry.get() instanceof com.gregtech.gregtech.block.OreBlock ore)
                    || ore.isSmall()) continue;
            ItemMaterialRegistry.register(ore.asItem(),
                    com.gregtech.gregtech.data.MaterialPrefixes.oreVanillastone.getDelegate(),
                    ore.material());
            count++;
        }

        // GT6 BlockGlowtus constructor: each colour carries one quarter-unit of Glowstone.
        // 1.20.1 has separate block items in place of GT6's 16 metadata values.
        for (String colour : com.gregtech.gregtech.worldgen.GTSurfaceFlora.GLOWTUS_COLOURS) {
            Block glowtus = com.gregtech.gregtech.worldgen.GTSurfaceFlora.glowtus(colour);
            if (glowtus == null) throw new IllegalStateException("Missing glowtus_" + colour);
            ItemMaterialRegistry.register(glowtus.asItem(), null,
                    com.gregtech.gregtech.content.material.Materials.Glowstone, GTValues.U4);
            count++;
        }

        // BlockFlowersA/B assign material only to A0/A1 (Wheat), B0/B1 (Acacia)
        // and B6 (Palm). Their ore indication is not a material composition.
        for (var flower : com.gregtech.gregtech.content.plant.BedrockFlowers.ALL) {
            if (flower.material() == null) continue;
            Block block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech", flower.id()));
            if (block == null || block == net.minecraft.world.level.block.Blocks.AIR)
                throw new IllegalStateException("Missing GT6 bedrock flower " + flower.id());
            ItemMaterialRegistry.register(block.asItem(), null, flower.material(), GTValues.U);
            count++;
        }

        GregTech.LOGGER.info("[{}] Registered {} GT block items for material tooltips", GregTech.NAMESPACE, count);
    }

    @SuppressWarnings("unchecked")
    private static int registerMachineList(List<?> list, long amount) {
        int count = 0;
        for (Object obj : list) {
            RegistryObject<? extends Block> entry = (RegistryObject<? extends Block>) obj;
            if (!entry.isPresent()) continue;
            Block block = entry.get();
            GTMaterial mat = extractMaterial(block);
            if (mat == null) continue;
            ItemMaterialRegistry.register(block.asItem(), null, mat, amount);
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static int registerSmelteryList(List<?> list, long amount) {
        int count = 0;
        for (Object obj : list) {
            RegistryObject<? extends Block> entry = (RegistryObject<? extends Block>) obj;
            if (!entry.isPresent()) continue;
            Block block = entry.get();
            GTMaterial mat = extractCrucibleMaterial(block);
            if (mat == null) continue;
            ItemMaterialRegistry.register(block.asItem(), null, mat, amount);
            count++;
        }
        return count;
    }

    private static GTMaterial extractMaterial(Block block) {
        if (block instanceof FluidPipeBlock pb) return pb.spec().material();
        if (block instanceof ItemPipeBlock pb) return pb.spec().material();
        if (block instanceof TankBlock tb) return tb.spec().material();
        return null;
    }

    private static GTMaterial extractCrucibleMaterial(Block block) {
        if (block instanceof MoldBlock mb) {
            CrucibleSpec spec = mb.spec();
            return spec != null ? spec.material() : null;
        }
        if (block instanceof MoldBasinBlock mb) {
            CrucibleSpec spec = mb.spec();
            return spec != null ? spec.material() : null;
        }
        if (block instanceof CrucibleCrossingBlock cc) {
            CrucibleSpec spec = cc.spec();
            return spec != null ? spec.material() : null;
        }
        if (block instanceof CrucibleFaucetBlock cf) {
            CrucibleSpec spec = cf.spec();
            return spec != null ? spec.material() : null;
        }
        if (block instanceof HopperBlock hb) {
            var spec = hb.spec();
            return spec != null ? spec.material() : null;
        }
        if (block instanceof QueueHopperBlock qb) {
            var spec = qb.spec();
            return spec != null ? spec.material() : null;
        }
        return null;
    }
}
