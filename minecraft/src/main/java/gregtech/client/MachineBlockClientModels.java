package gregtech.client;
import gregtech.engine.mod.GregTechIdentity;

import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.GTFacingMachineBlock;
import gregtech.content.block.machine.EngineBlock;
import gregtech.content.registry.GTBurningBoxes;
import gregtech.content.registry.GTEnergyNodes;
import gregtech.content.registry.GTEngines;
import gregtech.engine.machine.OrthogonalMachineModels;
import gregtech.platform.Mc;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Burning boxes, basic machines and the energy nodes: per-state model registration and aliasing.
 *
 * <p>These families bake from {@code machine_models.tsv} through {@link OrthogonalMachineBakedModel};
 * each row is baked once per rotation and reused by every block that shares it. Rows absent from
 * the table fall back to the bakery's static models, so both sources can coexist.
 */
public final class MachineBlockClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MachineBlockClientModels() {}

    public static void registerSharedModels(ModelEvent.RegisterAdditional event) {
        Set<ResourceLocation> registered = new HashSet<>();
        // Only rows of the table are additional models: an addon machine keeps its own static model,
        // and a row the table does not carry would fail this bake instead of falling back.
        for (BasicMachineBlock block : OrthogonalMachineFamilies.basicMachineBlocks()) {
            for (String suffix : new String[]{"", "_running", "_active"}) {
                ResourceLocation id = block.basicSpec().modelId(suffix);
                if (OrthogonalMachineFamilies.usesTable(id) && registered.add(id)) {
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
        // The shared pack serves a one-line spec model per table row, so the bakery always has one
        // baked instance to take this bake's sprites from.
        for (String modelPath : OrthogonalMachineModels.all().keySet()) {
            ClientModelEvents.registerAdditional(event, Mc.gregtechId(modelPath));
        }
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        // Per-bake only: a cached model belongs to this bake's sprites and atlas.
        Bakes bakes = new Bakes(models);
        Tally solid = new Tally();
        Tally fuel = new Tally();
        for (var entry : GTBurningBoxes.solidBoxes()) if (entry.isPresent()) burningBox(models, bakes, entry.get(), solid);
        for (var entry : GTBurningBoxes.fuelBoxes()) if (entry.isPresent()) burningBox(models, bakes, entry.get(), fuel);
        Tally basic = basicMachines(models, bakes);
        Tally nodes = energyNodes(models, bakes);
        int engines = engineItems(models);

        LOGGER.info("[{}] Machine model aliasing: {} solid burning boxes, {} new burning boxes, {} basic machines,"
                        + " {} energy nodes, {} engines ({} solid missing, {} new missing, {} basic missing,"
                        + " {} node missing)",
                GregTechIdentity.REGISTRY_NAMESPACE, solid.aliased, fuel.aliased, basic.aliased, nodes.aliased, engines,
                solid.missing, fuel.missing, basic.missing, nodes.missing);
    }

    private static void burningBox(BakedModelMap models, Bakes bakes, GTFacingMachineBlock block, Tally tally) {
        ResourceLocation blockId = Mc.blockId(block);
        if (blockId == null) return;

        Map<Direction, BakedModel> idle = facingModels(block, bakes, false);
        Map<Direction, BakedModel> active = facingModels(block, bakes, true);
        BakedModel itemInner = idle.get(Direction.NORTH);
        if (idle.size() != 4 || active.size() != 4 || itemInner == null) {
            tally.missing++;
            return;
        }
        // The shell and its detail pass are already tinted per layer, and the box switches between
        // the idle and the lit facing model, so the wrapper only picks one of the two. The item
        // shows the extinguished front, the row OrthogonalMachineFamilies picks for it.
        BakedModel worldModel = new BurningBoxBakedModel(idle, active, itemInner, itemInner.getParticleIcon());
        BakedModel itemModel = BlockItemClientModels.asBlockItem(itemInner);

        models.put(blockId, "", worldModel);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean lit : new boolean[]{false, true}) {
                models.put(blockId, blockVariant(facing, lit), worldModel);

                ResourceLocation sharedId = MachineBlockIcons.burningBoxVariant(facing, lit, isBrick(block));
                BakedModel shared = lit ? active.get(facing) : idle.get(facing);
                if (shared != null) {
                    models.put(sharedId, "", shared);
                }
            }
        }
        models.put(blockId, "inventory", itemModel);

        BlockItemClientModels.aliasBlockItem(models, block, itemModel);
        tally.aliased++;
    }

    private static Tally basicMachines(BakedModelMap models, Bakes bakes) {
        Tally tally = new Tally();
        for (BasicMachineBlock block : OrthogonalMachineFamilies.basicMachineBlocks()) {
            ResourceLocation blockId = Mc.blockId(block);
            if (blockId == null) continue;

            // Preserve the bakery's rotation for every actual blockstate: each lit/running
            // combination is its own table row or static model.
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BakedModel bakedState = bakes.world(block, state, OrthogonalMachineFamilies.world(state));
                if (bakedState != null) {
                    models.putState(blockId, state, bakedState);
                } else {
                    tally.missing++;
                    LOGGER.error("Missing machine state model: {}#{}", blockId, state);
                }
            }

            // The GUI keeps the working overlay; the hand, the ground and item frames show the idle
            // geometry, so an unplaced machine never looks busy.
            BakedModel guiInner = itemModel(bakes, block);
            if (guiInner == null) {
                tally.missing++;
                continue;
            }
            BakedModel idleInner =
                    bakes.get(OrthogonalMachineFamilies.Variant.flat(block.basicSpec().modelId("")));
            BakedModel itemModel = idleInner != null
                    ? BlockItemClientModels.asBlockItem(guiInner, idleInner)
                    : BlockItemClientModels.asBlockItem(guiInner);
            BlockItemClientModels.aliasBlockItem(models, block, itemModel);
            tally.aliased++;
        }
        return tally;
    }

    private static Tally energyNodes(BakedModelMap models, Bakes bakes) {
        Tally tally = new Tally();
        for (var entry : GTEnergyNodes.all()) {
            if (!entry.isPresent()) continue;

            // Solar panels and the plain energy nodes keep their own dedicated geometry.
            Block block = entry.get();
            ResourceLocation blockId = Mc.blockId(block);
            if (blockId == null || !OrthogonalMachineFamilies.sharedCube(entry.get())) continue;

            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BakedModel bakedState = bakes.world(block, state, OrthogonalMachineFamilies.world(state));
                if (bakedState != null) {
                    models.putState(blockId, state, bakedState);
                } else {
                    tally.missing++;
                    LOGGER.error("Missing energy node state model: {}#{}", blockId, state);
                }
            }

            BakedModel itemInner = itemModel(bakes, block);
            if (itemInner == null) continue;
            BlockItemClientModels.aliasBlockItem(models, block, BlockItemClientModels.asBlockItem(itemInner));
            tally.aliased++;
        }
        return tally;
    }

    /** Engine item models: wrap in BlockItemDisplayBakedModel for correct item transforms. */
    private static int engineItems(BakedModelMap models) {
        int aliased = 0;
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
                aliased++;
            }
        }
        return aliased;
    }

    /** First inventory row that exists; the item keeps its static model when none does. */
    private static @Nullable BakedModel itemModel(Bakes bakes, Block block) {
        for (OrthogonalMachineFamilies.Variant variant : OrthogonalMachineFamilies.itemVariants(block)) {
            BakedModel model = bakes.get(variant);
            if (model != null) return model;
        }
        return null;
    }

    /**
     * Burning boxes rotate the block state, not the geometry: each facing has its own row, which is
     * also the shared model id the old blockstates aliased.
     */
    private static Map<Direction, BakedModel> facingModels(GTFacingMachineBlock block, Bakes bakes, boolean lit) {
        Map<Direction, BakedModel> variants = new EnumMap<>(Direction.class);
        BlockState base = block.defaultBlockState();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState state = base.setValue(GTFacingMachineBlock.FACING, facing).setValue(GTFacingMachineBlock.LIT, lit);
            BakedModel model = bakes.get(OrthogonalMachineFamilies.world(state));
            if (model != null) {
                variants.put(facing, model);
            }
        }
        return variants;
    }

    private static boolean isBrick(GTFacingMachineBlock block) {
        return block.spec().id().contains("_brick");
    }

    private static String blockVariant(Direction facing, boolean lit) {
        return "facing=" + facing.getSerializedName() + ",lit=" + lit;
    }

    /** Counters for the single summary line. */
    private static final class Tally {
        private int aliased;
        private int missing;
    }

    /**
     * Bake-time model cache. A table row is baked once per rotation and every block state sharing
     * that row and rotation reuses the same quads; nothing here survives the bake, so a reload
     * cannot keep a stale sprite alive.
     */
    private static final class Bakes {
        private final BakedModelMap models;
        private final Map<Key, BakedModel> baked = new HashMap<>();
        private final Map<ResourceLocation, Optional<OrthogonalMachineBakedModel>> templates = new HashMap<>();

        private Bakes(BakedModelMap models) {
            this.models = models;
        }

        /** Model for a state: the table row when the table has one, otherwise the static model. */
        @Nullable
        BakedModel world(Block block, BlockState state, @Nullable OrthogonalMachineFamilies.Variant variant) {
            if (variant == null) return null;
            if (OrthogonalMachineFamilies.usesTable(variant.modelId())) return get(variant);
            // Migration fallback: the blockstate still points at the static model, rotation included.
            ResourceLocation blockId = Mc.blockId(block);
            if (blockId == null) return null;
            BakedModel staticModel =
                    models.get(blockId, BlockModelShaper.stateToModelLocation(blockId, state).getVariant());
            return models.isUsable(staticModel) ? staticModel : null;
        }

        /** Model for a row, independent of any block state; null when the row cannot be baked. */
        @Nullable
        BakedModel get(OrthogonalMachineFamilies.Variant variant) {
            Key key = new Key(variant.modelId(), variant.xRot(), variant.yRot(), variant.uvLock());
            BakedModel model = baked.get(key);
            if (model != null) return model;
            Optional<OrthogonalMachineBakedModel> template = template(variant.modelId());
            if (template.isEmpty()) {
                // Still on the static model, which the bakery already holds at its own rotation.
                BakedModel staticModel = models.find(variant.modelId());
                if (staticModel != null) baked.put(key, staticModel);
                return staticModel;
            }
            model = template.get().rebake(
                    OrthogonalMachineBakedModel.modelState(variant.xRot(), variant.yRot(), variant.uvLock()));
            baked.put(key, model);
            return model;
        }

        /** The row's own bake from the shared pack, whose sprites belong to this bake. */
        private Optional<OrthogonalMachineBakedModel> template(ResourceLocation modelId) {
            return templates.computeIfAbsent(modelId, id -> {
                BakedModel model = models.find(id);
                return model instanceof OrthogonalMachineBakedModel orthogonal
                        ? Optional.of(orthogonal)
                        : Optional.<OrthogonalMachineBakedModel>empty();
            });
        }

        private record Key(ResourceLocation modelId, int xRot, int yRot, boolean uvLock) {}
    }
}
