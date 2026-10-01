package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.BasicMachineSpec;
import com.gregtech.gregtech.api.machine.MachineRegistry;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MachineBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MachineBlockClientModels() {}

    private static final ResourceLocation BAROMETER_MODEL =
            GregTech.id("block/machines/barometer/sprites");

    @SubscribeEvent
    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        event.register(BAROMETER_MODEL);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            event.register(MachineBlockIcons.burningBoxVariant(facing, false, false));
            event.register(MachineBlockIcons.burningBoxVariant(facing, true, false));
            event.register(MachineBlockIcons.burningBoxVariant(facing, false, true));
            event.register(MachineBlockIcons.burningBoxVariant(facing, true, true));
            for (String directory : new String[]{"burning_liquid", "burning_gas", "burning_fluidbed"}) {
                for (String active : new String[]{"_on", "_off"}) {
                    event.register(GregTech.id("block/machine/" + directory + "/" + facing.getSerializedName() + active));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        int aliased = 0;
        int missing = 0;

        for (var entry : MachineRegistry.solidBurningBoxes()) {
            if (!entry.isPresent()) {
                continue;
            }
            SolidBurningBoxBlock block = entry.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
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
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
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
        for (var entry : MachineRegistry.burningBoxes()) {
            if (!entry.isPresent()) continue;
            BurningBoxBlock block = entry.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
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
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
            if (itemId == null) itemId = blockId;
            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);
            newBurningAliased++;
        }

        int basicAliased = 0;
        int basicMissing = 0;
        for (var entry : MachineRegistry.basicMachines()) {
            if (!entry.isPresent()) continue;
            BasicMachineBlock block = entry.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
            if (blockId == null) continue;
            BasicMachineSpec spec = block.basicSpec();

            // Idle model (base overlay, used for off state)
            ResourceLocation staticModelId = GregTech.id("block/machine/basic/" + spec.machineName());
            BakedModel staticModel = lookupModel(models, staticModelId);
            if (staticModel == null) {
                basicMissing++;
                continue;
            }
            BakedModel tintedIdleModel = new MaterialBlockBakedModel(staticModel);

            // Running model (standby overlay, used when powered but idle)
            ResourceLocation runningModelId = GregTech.id("block/machine/basic/" + spec.machineName() + "_running");
            BakedModel runningRaw = lookupModel(models, runningModelId);
            BakedModel tintedRunningModel = runningRaw != null ? new MaterialBlockBakedModel(runningRaw) : null;

            // Active model (working overlay, used when processing recipe)
            ResourceLocation activeModelId = GregTech.id("block/machine/basic/" + spec.machineName() + "_active");
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
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
            if (itemId == null) itemId = blockId;
            BlockItemClientModels.aliasItemInventory(models, itemId, itemModel);

            basicAliased++;
        }

        // Engine item models: wrap in BlockItemDisplayBakedModel for correct item transforms
        int engineAliased = 0;
        for (var entry : MachineRegistry.allEngines()) {
            if (!entry.isPresent()) continue;
            EngineBlock block = entry.get();
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(block.asItem());
            if (itemId == null) {
                itemId = ForgeRegistries.BLOCKS.getKey(block);
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
                GregTech.NAMESPACE, aliased, newBurningAliased, basicAliased, engineAliased, missing, newBurningMissing, basicMissing);
    }

    private static Map<Direction, BakedModel> loadFacingVariants(Map<ResourceLocation, BakedModel> models,
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

    private static Map<Direction, BakedModel> loadFuelTypeVariants(Map<ResourceLocation, BakedModel> models,
                                                                    String modelDir, boolean active) {
        Map<Direction, BakedModel> out = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            String variant = facing.getSerializedName() + (active ? "_on" : "_off");
            ResourceLocation id = GregTech.id("block/machine/" + modelDir + "/" + variant);
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

    private static BakedModel lookupModel(Map<ResourceLocation, BakedModel> models, ResourceLocation modelId) {
        return BakedModelLookup.find(models, modelId);
    }
}
