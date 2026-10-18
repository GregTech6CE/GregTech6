package com.gregtech.gregtech.client;







//? if >=1.21.1 {
/**///?} else {
import com.gregtech.gregtech.GregTech;

//?}


import com.gregtech.gregtech.api.machine.BasicMachineSpec;




//? if >=1.21.1 {
/**///?} else {

import com.gregtech.gregtech.api.machine.MachineRegistry;
//?}



import com.gregtech.gregtech.api.machine.BurningBoxFuelType;

import com.gregtech.gregtech.block.machine.BasicMachineBlock;

import com.gregtech.gregtech.block.machine.BurningBoxBlock;

import com.gregtech.gregtech.block.machine.EngineBlock;

import com.gregtech.gregtech.block.machine.SolidBurningBoxBlock;

import com.mojang.logging.LogUtils;

import net.minecraft.client.resources.model.BakedModel;

import net.minecraft.client.resources.model.ModelResourceLocation;

import net.minecraft.core.Direction;

import net.minecraft.resources.ResourceLocation;



//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
//?}

//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}

//? if >=1.21.1 {
/*import net.neoforged.bus.api.SubscribeEvent;
*///?} else {
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?}

//? if >=1.21.1 {
/*import net.neoforged.fml.common.EventBusSubscriber;
*///?} else {
import net.minecraftforge.fml.common.Mod;
//?}

//? if >=1.21.1 {
/*import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.registries.ForgeRegistries;
//?}



import org.slf4j.Logger;



import java.util.EnumMap;

import java.util.Map;





//? if >=1.21.1 {
/*@EventBusSubscriber(modid = com.gregtech.gregtech.api.mod.GregTechIdentity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}



public final class MachineBlockClientModels {

    private static final Logger LOGGER = LogUtils.getLogger();



    private MachineBlockClientModels() {}





//? if >=1.21.1 {
/**///?} else {
    private static final ResourceLocation BAROMETER_MODEL =

            GregTech.id("block/machines/barometer/sprites");



//?}


    @SubscribeEvent

    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {



//? if >=1.21.1 {
/*        java.util.Set<ResourceLocation> registered = new java.util.HashSet<>();
*///?} else {
        event.register(BAROMETER_MODEL);
//?}

//? if >=1.21.1 {
/*        for (var entry : com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries.all()) {
*///?} else {
        java.util.Set<ResourceLocation> shared = new java.util.HashSet<>();

        for (var entry : MachineRegistry.basicMachines()) {
//?}



            for (String suffix : new String[]{"", "_running", "_active"}) {


//? if >=1.21.1 {
/*                ResourceLocation id = entry.get().basicSpec().modelId(suffix);
*///?} else {
                var id = entry.get().basicSpec().modelId(suffix);
//?}


//? if >=1.21.1 {
/*                if (registered.add(id)) event.register(ModelResourceLocation.standalone(id));
*///?} else {
                if (shared.add(id)) event.register(id);
//?}



            }

        }

        for (Direction facing : Direction.Plane.HORIZONTAL) {



//? if >=1.21.1 {
/*            event.register(ModelResourceLocation.standalone(MachineBlockIcons.burningBoxVariant(facing, false, false)));
*///?} else {
            event.register(MachineBlockIcons.burningBoxVariant(facing, false, false));
//?}

//? if >=1.21.1 {
/*            event.register(ModelResourceLocation.standalone(MachineBlockIcons.burningBoxVariant(facing, true, false)));
*///?} else {
            event.register(MachineBlockIcons.burningBoxVariant(facing, true, false));
//?}

//? if >=1.21.1 {
/*            event.register(ModelResourceLocation.standalone(MachineBlockIcons.burningBoxVariant(facing, false, true)));
*///?} else {
            event.register(MachineBlockIcons.burningBoxVariant(facing, false, true));
//?}

//? if >=1.21.1 {
/*            event.register(ModelResourceLocation.standalone(MachineBlockIcons.burningBoxVariant(facing, true, true)));
*///?} else {
            event.register(MachineBlockIcons.burningBoxVariant(facing, true, true));
//?}



            for (String directory : new String[]{"burning_liquid", "burning_gas", "burning_fluidbed"}) {

                for (String active : new String[]{"_on", "_off"}) {



//? if >=1.21.1 {
/*                    event.register(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("gt6ce","block/machine/" + directory + "/" + facing.getSerializedName() + active)));
*///?} else {
                    event.register(GregTech.id("block/machine/" + directory + "/" + facing.getSerializedName() + active));
//?}



                }

            }

        }

    }



    @SubscribeEvent

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {



//? if >=1.21.1 {
/*        Map<ModelResourceLocation, BakedModel> models = event.getModels();
*///?} else {
        Map<ResourceLocation, BakedModel> models = event.getModels();
//?}



        BlockItemModelHelper.captureFromVanillaBlockItem(models);

        int aliased = 0;

        int missing = 0;





//? if >=1.21.1 {
/*        for (var entry : com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries.solidBoxes()) {
*///?} else {
        for (var entry : MachineRegistry.solidBurningBoxes()) {
//?}

//? if >=1.21.1 {
/*            if (!entry.isBound()) {
*///?} else {
            if (!entry.isPresent()) {
//?}



                continue;

            }

            SolidBurningBoxBlock block = entry.get();



//? if >=1.21.1 {
/*            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
//?}



            if (blockId == null) {

                continue;

            }

            boolean brick = blockId.getPath().contains("_brick");

            Map<Direction, BakedModel> idleBase = loadFacingVariants(models, false, brick);

            Map<Direction, BakedModel> activeBase = loadFacingVariants(models, true, brick);

            if (idleBase.size() != 4 || activeBase.size() != 4) {

                missing++;

                continue;

            }

            BakedModel itemInner = idleBase.get(Direction.NORTH);

            if (itemInner == null) {

                missing++;

                continue;

            }

            BakedModel tintedItemInner = new MaterialBlockBakedModel(itemInner);

            BakedModel worldModel = new BurningBoxBakedModel(idleBase, activeBase, tintedItemInner);

            BakedModel itemModel = BlockItemClientModels.asBlockItem(tintedItemInner);



            models.put(new ModelResourceLocation(blockId, ""), worldModel);

            models.put(new ModelResourceLocation(blockId, "waterlogged=false"), worldModel);

            models.put(new ModelResourceLocation(blockId, "waterlogged=true"), worldModel);

            for (Direction facing : Direction.Plane.HORIZONTAL) {

                for (boolean lit : new boolean[]{false, true}) {

                    models.put(blockVariant(blockId, facing, lit), worldModel);

                    models.put(blockVariantW(blockId, facing, lit, false), worldModel);

                    models.put(blockVariantW(blockId, facing, lit, true), worldModel);

                    ResourceLocation sharedId = MachineBlockIcons.burningBoxVariant(facing, lit, brick);

                    BakedModel shared = lit ? activeBase.get(facing) : idleBase.get(facing);

                    if (shared != null) {

                        models.put(new ModelResourceLocation(sharedId, ""), shared);

                    }

                }

            }



//? if >=1.21.1 {
/*            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(block.asItem());
*///?} else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
//?}



            if (itemId == null) {

                itemId = blockId;

            }

            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);

            aliased++;

        }



        // New burning boxes (liquid/gas/fluidized-bed) — same pattern as solid but with

        // per-fuel-type model directories instead of brick/solid shells.

        int newBurningAliased = 0;

        int newBurningMissing = 0;



//? if >=1.21.1 {
/*        for (var entry : com.gregtech.gregtech.platform.neoforge.smeltery.SmelteryRegistries.fuelBoxes()) {
*///?} else {
        for (var entry : MachineRegistry.burningBoxes()) {
//?}

//? if >=1.21.1 {
/*            if (!entry.isBound()) continue;
*///?} else {
            if (!entry.isPresent()) continue;
//?}



            BurningBoxBlock block = entry.get();



//? if >=1.21.1 {
/*            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
//?}



            if (blockId == null) continue;



            BurningBoxFuelType fuelType = block.fuelType();

            String modelDir = switch (fuelType) {

                case LIQUID -> "burning_liquid";

                case GAS -> "burning_gas";

                case FLUIDIZED_BED -> "burning_fluidbed";

                default -> null;

            };

            if (modelDir == null) continue;



            Map<Direction, BakedModel> idleBase = loadFuelTypeVariants(models, modelDir, false);

            Map<Direction, BakedModel> activeBase = loadFuelTypeVariants(models, modelDir, true);

            if (idleBase.size() != 4 || activeBase.size() != 4) {

                newBurningMissing++;

                continue;

            }

            BakedModel itemInner = idleBase.get(Direction.NORTH);

            if (itemInner == null) {

                newBurningMissing++;

                continue;

            }

            BakedModel tintedItemInner = new MaterialBlockBakedModel(itemInner);

            BakedModel worldModel = new BurningBoxBakedModel(idleBase, activeBase, tintedItemInner);

            BakedModel itemModel = BlockItemClientModels.asBlockItem(tintedItemInner);



            models.put(new ModelResourceLocation(blockId, ""), worldModel);

            models.put(new ModelResourceLocation(blockId, "waterlogged=false"), worldModel);

            models.put(new ModelResourceLocation(blockId, "waterlogged=true"), worldModel);

            for (Direction facing : Direction.Plane.HORIZONTAL) {

                for (boolean lit : new boolean[]{false, true}) {

                    models.put(blockVariant(blockId, facing, lit), worldModel);

                    models.put(blockVariantW(blockId, facing, lit, false), worldModel);

                    models.put(blockVariantW(blockId, facing, lit, true), worldModel);

                }

            }

            models.put(new ModelResourceLocation(blockId, "inventory"), itemModel);



//? if >=1.21.1 {
/*            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(block.asItem());
*///?} else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
//?}



            if (itemId == null) itemId = blockId;

            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);

            newBurningAliased++;

        }



        int basicAliased = 0;

        int basicMissing = 0;



//? if >=1.21.1 {
/*        for (var entry : com.gregtech.gregtech.platform.neoforge.machine.BasicMachineRegistries.all()) {
*///?} else {
        for (var entry : MachineRegistry.basicMachines()) {
//?}

//? if >=1.21.1 {
/*            if (!entry.isBound()) continue;
*///?} else {
            if (!entry.isPresent()) continue;
//?}



            BasicMachineBlock block = entry.get();



//? if >=1.21.1 {
/*            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
//?}



            if (blockId == null) continue;

            BasicMachineSpec spec = block.basicSpec();



            // Idle model (base overlay, used for off state)


            ResourceLocation staticModelId = spec.modelId("");


            BakedModel staticModel = lookupModel(models, staticModelId);

            if (staticModel == null) {

                basicMissing++;

                continue;

            }

            BakedModel tintedIdleModel = new MaterialBlockBakedModel(staticModel);



            // Running model (standby overlay, used when powered but idle)


            ResourceLocation runningModelId = spec.modelId("_running");


            BakedModel runningRaw = lookupModel(models, runningModelId);

            BakedModel tintedRunningModel = runningRaw != null ? new MaterialBlockBakedModel(runningRaw) : null;



            // Active model (working overlay, used when processing recipe)


            ResourceLocation activeModelId = spec.modelId("_active");


            BakedModel activeRaw = lookupModel(models, activeModelId);

            BakedModel tintedActiveModel = activeRaw != null ? new MaterialBlockBakedModel(activeRaw) : null;



            // Preserve the bakery's rotation for every actual blockstate.

            for (var state : block.getStateDefinition().getPossibleStates()) {

                ModelResourceLocation key = net.minecraft.client.renderer.block.BlockModelShaper

                        .stateToModelLocation(blockId, state);

                BakedModel bakedState = models.get(key);

                if (BakedModelLookup.isUsable(models, bakedState)) {

                    models.put(key, new MaterialBlockBakedModel(bakedState));

                } else {

                    basicMissing++;

                    LOGGER.error("Missing machine state model: {}", key);

                }

            }



            // Item model (use active variant for inventory preview)

            BakedModel itemInner = tintedActiveModel != null ? tintedActiveModel : tintedIdleModel;

            BakedModel itemModel = BlockItemClientModels.asBlockItem(itemInner);



//? if >=1.21.1 {
/*            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(block.asItem());
*///?} else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
//?}



            if (itemId == null) itemId = blockId;

            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);



            basicAliased++;

        }



        // Engine item models: wrap in BlockItemDisplayBakedModel for correct item transforms

        int engineAliased = 0;



//? if >=1.21.1 {
/*        for (var entry : com.gregtech.gregtech.registry.GTEngines.all()) {
*///?} else {
        for (var entry : MachineRegistry.allEngines()) {
//?}

//? if >=1.21.1 {
/*            if (!entry.isBound()) continue;
*///?} else {
            if (!entry.isPresent()) continue;
//?}



            EngineBlock block = entry.get();



//? if >=1.21.1 {
/*            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(block.asItem());
*///?} else {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
//?}



            if (itemId == null) {



//? if >=1.21.1 {
/*                itemId = BuiltInRegistries.BLOCK.getKey(block);
*///?} else {
                itemId = ForgeRegistries.BLOCKS.getKey(block);
//?}



            }

            if (itemId == null) continue;



            BakedModel existing = models.get(new ModelResourceLocation(itemId, "inventory"));

            if (existing != null && !(existing instanceof BlockItemDisplayBakedModel)) {

                models.put(new ModelResourceLocation(itemId, "inventory"),

                        new BlockItemDisplayBakedModel(existing));

                engineAliased++;

            }

        }



        LOGGER.info("[{}] Machine model aliasing: {} solid burning boxes, {} new burning boxes, {} basic machines, {} engines ({} solid missing, {} new missing, {} basic missing)",



//? if >=1.21.1 {
/*                "gt6ce", aliased, newBurningAliased, basicAliased, engineAliased, missing, newBurningMissing, basicMissing);
*///?} else {
                GregTech.NAMESPACE, aliased, newBurningAliased, basicAliased, engineAliased, missing, newBurningMissing, basicMissing);
//?}



    }





//? if >=1.21.1 {
/*    private static Map<Direction, BakedModel> loadFacingVariants(Map<ModelResourceLocation, BakedModel> models,
*///?} else {
    private static Map<Direction, BakedModel> loadFacingVariants(Map<ResourceLocation, BakedModel> models,
//?}



                                                                 boolean active, boolean brick) {

        Map<Direction, BakedModel> out = new EnumMap<>(Direction.class);

        for (Direction facing : Direction.Plane.HORIZONTAL) {

            BakedModel model = lookupModel(models, MachineBlockIcons.burningBoxVariant(facing, active, brick));

            if (model != null) {

                out.put(facing, model);

            }

        }

        return out;

    }





//? if >=1.21.1 {
/*    private static Map<Direction, BakedModel> loadFuelTypeVariants(Map<ModelResourceLocation, BakedModel> models,
*///?} else {
    private static Map<Direction, BakedModel> loadFuelTypeVariants(Map<ResourceLocation, BakedModel> models,
//?}



                                                                    String modelDir, boolean active) {

        Map<Direction, BakedModel> out = new EnumMap<>(Direction.class);

        for (Direction facing : Direction.Plane.HORIZONTAL) {

            String variant = facing.getSerializedName() + (active ? "_on" : "_off");



//? if >=1.21.1 {
/*            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gt6ce","block/machine/" + modelDir + "/" + variant);
*///?} else {
            ResourceLocation id = GregTech.id("block/machine/" + modelDir + "/" + variant);
//?}



            BakedModel model = lookupModel(models, id);

            if (model != null) {

                out.put(facing, model);

            }

        }

        return out;

    }



    private static ModelResourceLocation blockVariant(ResourceLocation blockId, Direction facing, boolean lit) {

        return new ModelResourceLocation(blockId,

                "facing=" + facing.getSerializedName() + ",lit=" + lit);

    }



    private static ModelResourceLocation blockVariant(ResourceLocation blockId, Direction facing, boolean lit, boolean running) {

        return new ModelResourceLocation(blockId,

                "facing=" + facing.getSerializedName() + ",lit=" + lit + ",running=" + running);

    }



    private static ModelResourceLocation blockVariantW(ResourceLocation blockId, Direction facing, boolean lit, boolean waterlogged) {

        return new ModelResourceLocation(blockId,

                "facing=" + facing.getSerializedName() + ",lit=" + lit + ",waterlogged=" + waterlogged);

    }



    private static ModelResourceLocation blockVariantW(ResourceLocation blockId, Direction facing, boolean lit, boolean running, boolean waterlogged) {

        return new ModelResourceLocation(blockId,

                "facing=" + facing.getSerializedName() + ",lit=" + lit + ",running=" + running + ",waterlogged=" + waterlogged);

    }





//? if >=1.21.1 {
/*    private static BakedModel lookupModel(Map<ModelResourceLocation, BakedModel> models, ResourceLocation modelId) {
*///?} else {
    private static BakedModel lookupModel(Map<ResourceLocation, BakedModel> models, ResourceLocation modelId) {
//?}



        return BakedModelLookup.find(models, modelId);

    }

}
