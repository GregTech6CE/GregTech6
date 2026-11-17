package gregtech.client;

import gregtech.content.block.energy.EnergyNodeBlock;
import gregtech.content.block.machine.BasicMachineBlock;
import gregtech.content.block.machine.EngineBlock;
import gregtech.content.registry.GTBurningBoxes;
import gregtech.content.registry.GTEnergyNodes;
import gregtech.content.registry.GTEngines;
import gregtech.engine.machine.OrthogonalMachineModels;
import gregtech.engine.mod.GregTechIdentity;
import gregtech.platform.Mc;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.event.ModelEvent;
*///?} else {
import net.minecraftforge.client.event.ModelEvent;
//?}
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Burning boxes, basic machines and the energy nodes: the inventory models those families keep.
 *
 * <p>Their world geometry needs no pass here. The shared pack serves a real blockstate per state, so
 * the vanilla bakery bakes every one of them from {@code machine_models.tsv} through
 * {@link OrthogonalMachineModelLoader}, at the rotation the blockstate names. Only the item display
 * is decided below, and only for the items whose {@code models/item/<id>.json} the shared pack still
 * serves.
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

        // The shared pack serves a one-line spec model per table row, so the bakery always has one
        // baked instance to take this bake's sprites from.
        for (String modelPath : OrthogonalMachineModels.all().keySet()) {
            ClientModelEvents.registerAdditional(event, Mc.gregtechId(modelPath));
        }
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        BakedModelMap models = BakedModelMap.of(event.getModels());
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resources = minecraft == null ? null : minecraft.getResourceManager();
        Tally boxes = new Tally();
        for (var entry : GTBurningBoxes.solidBoxes()) {
            if (entry.isPresent() && OrthogonalMachineFamilies.usesTable(entry.get())) {
                aliasIdleBlockItem(models, resources, entry.get(), boxes);
            }
        }
        for (var entry : GTBurningBoxes.fuelBoxes()) {
            if (entry.isPresent() && OrthogonalMachineFamilies.usesTable(entry.get())) {
                aliasIdleBlockItem(models, resources, entry.get(), boxes);
            }
        }
        Tally basic = basicMachines(models, resources);
        Tally nodes = energyNodes(models, resources);
        int engines = engineItems(models);

        LOGGER.info("[{}] Machine item display: {} burning boxes, {} basic machines, {} energy nodes, {} engines"
                        + " ({} not served by the shared pack, {} burning missing, {} basic missing, {} node missing)",
                GregTechIdentity.REGISTRY_NAMESPACE, boxes.aliased, basic.aliased, nodes.aliased, engines,
                boxes.kept + basic.kept + nodes.kept, boxes.missing, basic.missing, nodes.missing);
    }

    /**
     * Idle-only machine items (burning boxes and shared-cube energy nodes): wraps the block row in
     * item transforms while the shared pack still serves this item's virtual row model.
     */
    private static void aliasIdleBlockItem(BakedModelMap models, @Nullable ResourceManager resources,
                                           Block block, Tally tally) {
        ResourceLocation itemId = itemId(block);
        if (itemId == null) return;
        if (!isVirtualItemModel(resources, itemId)) {
            tally.kept++;
            return;
        }
        BakedModel itemInner = itemModel(models, block);
        if (itemInner == null) {
            tally.missing++;
            return;
        }
        BlockItemClientModels.aliasBlockItem(models, block, BlockItemClientModels.asBlockItem(itemInner));
        tally.aliased++;
    }

    private static Tally basicMachines(BakedModelMap models, @Nullable ResourceManager resources) {
        Tally tally = new Tally();
        for (BasicMachineBlock block : OrthogonalMachineFamilies.basicMachineBlocks()) {
            ResourceLocation itemId = itemId(block);
            if (itemId == null) continue;
            if (!isVirtualItemModel(resources, itemId)) {
                tally.kept++;
                continue;
            }

            // The GUI keeps the working overlay; the hand, the ground and item frames show the idle
            // geometry, so an unplaced machine never looks busy.
            BakedModel guiInner = itemModel(models, block);
            if (guiInner == null) {
                tally.missing++;
                continue;
            }
            BakedModel idleInner = models.find(block.basicSpec().modelId(""));
            BlockItemClientModels.aliasBlockItem(
                    models, block, BlockItemClientModels.asBlockItem(guiInner, idleInner != null ? idleInner : guiInner));
            tally.aliased++;
        }
        return tally;
    }

    private static Tally energyNodes(BakedModelMap models, @Nullable ResourceManager resources) {
        Tally tally = new Tally();
        for (var entry : GTEnergyNodes.all()) {
            if (!entry.isPresent()) continue;

            // Solar panels and the plain energy nodes keep their own dedicated geometry.
            EnergyNodeBlock node = entry.get();
            if (OrthogonalMachineFamilies.sharedCube(node)) {
                aliasIdleBlockItem(models, resources, node, tally);
            }
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

    /**
     * First inventory row that exists; the item keeps its static model when none does. The rows are
     * looked up in the bakery, so a resource pack that replaced a row model is honoured.
     */
    private static @Nullable BakedModel itemModel(BakedModelMap models, Block block) {
        for (OrthogonalMachineFamilies.Variant variant : OrthogonalMachineFamilies.itemVariants(block)) {
            BakedModel model = models.find(variant.modelId());
            if (model != null) return model;
        }
        return null;
    }

    /** The block's own item id, or null when the block has no item. */
    private static @Nullable ResourceLocation itemId(Block block) {
        return block.asItem() == Items.AIR ? null : Mc.itemId(block.asItem());
    }

    /**
     * True while the shared pack itself serves {@code models/item/<id>.json}, i.e. while this item
     * still uses the virtual row model this callback fills in. A pack that {@code filter}s that
     * entry away, or replaces it with its own item model, decides the item's model itself and must
     * not have one put back here.
     */
    private static boolean isVirtualItemModel(@Nullable ResourceManager resources, ResourceLocation itemId) {
        if (resources == null) return false;
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(), "models/item/" + itemId.getPath() + ".json");
        return resources.getResource(path)
                .map(resource -> SharedInventoryModelPack.PACK_ID.equals(resource.sourcePackId()))
                .orElse(false);
    }

    /** Counters for the single summary line. */
    private static final class Tally {
        private int aliased;
        private int kept;
        private int missing;
    }
}
