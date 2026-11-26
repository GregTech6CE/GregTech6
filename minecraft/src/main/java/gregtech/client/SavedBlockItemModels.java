package gregtech.client;

import gregtech.content.block.energy.ZpmDischargerBlock;
import gregtech.content.block.tool.DustFunnelBlock;
import gregtech.content.blockentity.energy.ZpmDischargerBlockEntity;
import gregtech.content.registry.GTLasers;
import gregtech.content.registry.GTManualStations;
import java.util.function.IntFunction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Selects the placed geometry represented by a block item's saved data. */
final class SavedBlockItemModels {
    private SavedBlockItemModels() {}

    static void models(BakedModelMap models) {
        var funnel = GTManualStations.DUST_FUNNEL.get();
        register(
                models,
                funnel,
                DustFunnelBlock.MODE.getPossibleValues().size(),
                mode -> funnel.defaultBlockState().setValue(DustFunnelBlock.MODE, mode),
                (stack, level) -> DustFunnelBlock.itemMode(stack));
        for (var registered : java.util.List.of(GTLasers.ZPM_DISCHARGER_BASIC, GTLasers.ZPM_DISCHARGER_ADVANCED)) {
            var discharger = registered.get();
            register(
                    models,
                    discharger,
                    2,
                    loaded -> discharger.defaultBlockState().setValue(ZpmDischargerBlock.LOADED, loaded != 0),
                    (stack, level) -> ZpmDischargerBlockEntity.hasPackedModule(
                                    stack, level == null ? null : level.registryAccess())
                            ? 1
                            : 0);
        }
    }

    private static void register(
            BakedModelMap models,
            Block block,
            int variants,
            IntFunction<BlockState> stateFor,
            VariantSelector selector) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        var item = models.get(id, "inventory");
        if (item == null) return;

        var geometry = new BakedModel[variants];
        for (int variant = 1; variant < variants; variant++) {
            geometry[variant] = BlockItemModelHelper.applyContext(
                    models.getState(id, stateFor.apply(variant)),
                    item.getTransforms(),
                    ItemOverrides.EMPTY,
                    item.useAmbientOcclusion(),
                    item.usesBlockLight());
        }

        var overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(
                    BakedModel original,
                    ItemStack stack,
                    @Nullable ClientLevel level,
                    @Nullable LivingEntity entity,
                    int seed) {
                var base = item.getOverrides().resolve(item, stack, level, entity, seed);
                if (base != item) return base;
                var selected = geometry[selector.variant(stack, level)];
                return selected == null ? item : selected;
            }
        };
        models.put(
                id,
                "inventory",
                BlockItemModelHelper.applyContext(
                        item, item.getTransforms(), overrides, item.useAmbientOcclusion(), item.usesBlockLight()));
    }

    @FunctionalInterface
    private interface VariantSelector {
        int variant(ItemStack stack, @Nullable ClientLevel level);
    }
}
