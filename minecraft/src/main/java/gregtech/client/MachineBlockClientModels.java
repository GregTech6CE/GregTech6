package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;

import gregtech.engine.machine.BasicMachineSpec;

import gregtech.engine.machine.BurningBoxFuelType;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.BurningBoxBlock;
import gregtech.content.block.machine.EngineBlock;
import gregtech.content.block.machine.SolidBurningBoxBlock;
import gregtech.content.registry.GTBasicMachines;
import gregtech.content.registry.GTBurningBoxes;
import gregtech.content.registry.GTEngines;
import gregtech.platform.Mc;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Burning boxes, basic machines and engines: shared model registration and per-state aliasing. */
public final class MachineBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MachineBlockClientModels() {}

//? if >=1.21.1 {
//?} else {
    private static final ResourceLocation BAROMETER_MODEL =
            ResourceLocation.fromNamespaceAndPath(GregTechIdentity.REGISTRY_NAMESPACE, ("block/machines/barometer/sprites").toLowerCase());
//?}

    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        Set<ResourceLocation> registered = new HashSet<>();
//? if >=1.21.1 {
//?} else {
        ClientModelEvents.registerAdditional(event, BAROMETER_MODEL);
//?}
        for (var entry : GTBasicMachines.all()) {
            for (String suffix : new String[]{"", "_running", "_active"}) {
                ResourceLocation id = entry.get().basicSpec().modelId(suffix);
                if (registered.add(id)) {
                    ClientModelEvents.registerAdditional(event, id);
                }
            }
        }

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            ClientModelEvents.registerAdditional(event, MachineBlockIcons.burningBoxVariant(facing, false, false));
            ClientModelEvents.registerAdditional(event, MachineBlockIcons.burningBoxVariant(facing, true, false));
            ClientModelEvents.registerAdditional(event, MachineBlockIcons.burningBoxVariant(facing, false, true));
            ClientModelEvents.registerAdditional(event, MachineBlockIcons.burningBoxVariant(facing, true, true));

            for (String directory : new String[]{"burning_liquid", "burning_gas", "burning_fluidbed"}) {
                for (String active : new String[]{"_on", "_off"}) {
                    ClientModelEvents.registerAdditional(event, Mc.gregtechId(
                            "block/machine/" + directory + "/" + facing.getSerializedName() + active));
                }
            }
        }
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        int aliased = 0;
        int missing = 0;

        for (var entry : GTBurningBoxes.solidBoxes()) {
            if (!entry.isPresent()) {
                continue;
            }
            SolidBurningBoxBlock block = entry.get();

            ResourceLocation blockId = Mc.blockId(block);
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

            models.put(blockId, "", worldModel);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                for (boolean lit : new boolean[]{false, true}) {
                    models.put(blockId, blockVariant(facing, lit), worldModel);

                    ResourceLocation sharedId = MachineBlockIcons.burningBoxVariant(facing, lit, brick);
                    BakedModel shared = lit ? activeBase.get(facing) : idleBase.get(facing);
                    if (shared != null) {
                        models.put(sharedId, "", shared);
                    }
                }
            }

            BlockItemClientModels.aliasBlockItem(models, block, itemModel);
            aliased++;
        }

        // New burning boxes (liquid/gas/fluidized-bed) — same pattern as solid but with
        // per-fuel-type model directories instead of brick/solid shells.
        int newBurningAliased = 0;
        int newBurningMissing = 0;

        for (var entry : GTBurningBoxes.fuelBoxes()) {
            if (!entry.isPresent()) continue;

            BurningBoxBlock block = entry.get();

            ResourceLocation blockId = Mc.blockId(block);
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

            models.put(blockId, "", worldModel);
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                for (boolean lit : new boolean[]{false, true}) {
                    models.put(blockId, blockVariant(facing, lit), worldModel);
                }
            }
            models.put(blockId, "inventory", itemModel);

            BlockItemClientModels.aliasBlockItem(models, block, itemModel);
            newBurningAliased++;
        }

        int basicAliased = 0;
        int basicMissing = 0;

        for (var entry : GTBasicMachines.all()) {
            if (!entry.isPresent()) continue;

            BasicMachineBlock block = entry.get();

            ResourceLocation blockId = Mc.blockId(block);
            if (blockId == null) continue;
            BasicMachineSpec spec = block.basicSpec();

            // Idle model (base overlay, used for off state)
            ResourceLocation staticModelId = spec.modelId("");
            BakedModel staticModel = models.find(staticModelId);
            if (staticModel == null) {
                basicMissing++;
                continue;
            }
            BakedModel tintedIdleModel = new MaterialBlockBakedModel(staticModel);

            // Running model (standby overlay, used when powered but idle)
            ResourceLocation runningModelId = spec.modelId("_running");
            BakedModel runningRaw = models.find(runningModelId);
            BakedModel tintedRunningModel = runningRaw != null ? new MaterialBlockBakedModel(runningRaw) : null;

            // Active model (working overlay, used when processing recipe)
            ResourceLocation activeModelId = spec.modelId("_active");
            BakedModel activeRaw = models.find(activeModelId);
            BakedModel tintedActiveModel = activeRaw != null ? new MaterialBlockBakedModel(activeRaw) : null;

            // Preserve the bakery's rotation for every actual blockstate.
            for (var state : block.getStateDefinition().getPossibleStates()) {
                String variant = BlockModelShaper.stateToModelLocation(blockId, state).getVariant();
                BakedModel bakedState = models.get(blockId, variant);
                if (models.isUsable(bakedState)) {
                    models.putState(blockId, state, new MaterialBlockBakedModel(bakedState));
                } else {
                    basicMissing++;
                    LOGGER.error("Missing machine state model: {}#{}", blockId, variant);
                }
            }

            // Item model (use active variant for inventory preview)
            BakedModel itemInner = tintedActiveModel != null ? tintedActiveModel : tintedIdleModel;
            BakedModel itemModel = BlockItemClientModels.asBlockItem(itemInner);

            BlockItemClientModels.aliasBlockItem(models, block, itemModel);

            basicAliased++;
        }

        // Engine item models: wrap in BlockItemDisplayBakedModel for correct item transforms
        int engineAliased = 0;

        for (var entry : GTEngines.all()) {
            if (!entry.isPresent()) continue;

            EngineBlock block = entry.get();

            ResourceLocation itemId = Mc.itemId(block.asItem());
            if (itemId == null) {
                itemId = Mc.blockId(block);
            }
            if (itemId == null) continue;

            BakedModel existing = models.get(itemId, "inventory");
            if (existing != null && !(existing instanceof BlockItemDisplayBakedModel)) {
                models.put(itemId, "inventory", new BlockItemDisplayBakedModel(existing));
                engineAliased++;
            }
        }

        LOGGER.info("[{}] Machine model aliasing: {} solid burning boxes, {} new burning boxes, {} basic machines, {} engines ({} solid missing, {} new missing, {} basic missing)",
                GregTechIdentity.REGISTRY_NAMESPACE, aliased, newBurningAliased, basicAliased, engineAliased, missing, newBurningMissing, basicMissing);
    }

    private static Map<Direction, BakedModel> loadFacingVariants(BakedModelMap models, boolean active, boolean brick) {
        Map<Direction, BakedModel> out = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BakedModel model = models.find(MachineBlockIcons.burningBoxVariant(facing, active, brick));
            if (model != null) {
                out.put(facing, model);
            }
        }
        return out;
    }

    private static Map<Direction, BakedModel> loadFuelTypeVariants(BakedModelMap models, String modelDir, boolean active) {
        Map<Direction, BakedModel> out = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            String variant = facing.getSerializedName() + (active ? "_on" : "_off");
            BakedModel model = models.find(Mc.gregtechId("block/machine/" + modelDir + "/" + variant));
            if (model != null) {
                out.put(facing, model);
            }
        }
        return out;
    }

    private static String blockVariant(Direction facing, boolean lit) {
        return "facing=" + facing.getSerializedName() + ",lit=" + lit;
    }
}
