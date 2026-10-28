package gregtech.client;

import gregtech.content.block.RockBlock;
import gregtech.content.blockentity.RockBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.List;
//? if >=1.21.1 {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
@EventBusSubscriber(modid = "gt6ce", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
*///?} else {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
@Mod.EventBusSubscriber(modid = "gt6ce", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//?}
public final class RawOreRockModels {
    private RawOreRockModels() {}
    private static ResourceLocation marker(RockBlock.Ground ground) {
        return ResourceLocation.fromNamespaceAndPath("gt6ce", "block/rock_marker_" + ground.getSerializedName());
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        for (var ground : RockBlock.Ground.values()) {
//? if >=1.21.1 {
            /*event.register(ModelResourceLocation.standalone(marker(ground)));
*///?} else {
            event.register(marker(ground));
//?}
        }
    }

//? if >=1.21.1 {
    /*@SubscribeEvent
    public static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
        // Forge already registers this material tint in GregTechClient. Only the new raw-marker
        // geometry consumes tint 0; ordinary ground-pebble models remain unchanged.
        event.register((state, level, pos, tint) -> {
            if (tint == 0 && level != null && pos != null
                    && level.getBlockEntity(pos) instanceof RockBlockEntity rock && rock.hasRawOre()) {
                var material = gregtech.engine.material.GTMaterialRegistry.get(rock.getMaterial());
                if (material != null && material.resolve().isValid()) return material.resolve().getColor();
            }
            return 0xFFFFFF;
        }, gregtech.content.registry.GTBlocks.ROCK.get());
    }
*///?}

    @SubscribeEvent
    public static void bake(ModelEvent.ModifyBakingResult event) {
        for (var ground : RockBlock.Ground.values()) {
            var single = BakedModelLookup.find(event.getModels(), marker(ground));
            if (single == null) continue;
            var id = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath("gt6ce", "rock"),
                    "ground=" + ground.getSerializedName());
            event.getModels().computeIfPresent(id, (key, normal) -> new MarkerModel(normal, single));
        }
    }

    /** Preserve ordinary pebble clusters; only the saved rawOre marker mode uses one large chunk. */
    public static final class MarkerModel extends BakedModelWrapper<BakedModel> {
        private final BakedModel single;
        public MarkerModel(BakedModel normal, BakedModel single) { super(normal); this.single = single; }
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                ModelData data, @Nullable RenderType layer) {
            return (Boolean.TRUE.equals(data.get(RockBlockEntity.RAW_ORE_PROPERTY)) ? single : originalModel)
                    .getQuads(state, side, random, data, layer);
        }
    }
}
