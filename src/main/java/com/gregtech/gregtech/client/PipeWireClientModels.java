package com.gregtech.gregtech.client;

import com.gregtech.gregtech.GregTech;
import com.gregtech.gregtech.api.machine.ItemPipeSpec;
import com.gregtech.gregtech.api.machine.PipeSpec;
import com.gregtech.gregtech.api.energy.WireSpec;
import com.gregtech.gregtech.block.energy.AxleBlock;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.block.machine.ItemPipeBlock;
import com.gregtech.gregtech.registry.GTAxles;
import com.gregtech.gregtech.registry.GTFluidPipes;
import com.gregtech.gregtech.registry.GTItemPipes;
import com.gregtech.gregtech.registry.GTWires;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Map;

/**
 * Replaces every pipe/wire/cable blockstate variant with the dynamic
 * {@link PipeWireBakedModel} (GT6 thin-into-thick connection rendering).
 * Items use the same geometry with two opposite connection arms.
 */
@Mod.EventBusSubscriber(modid = GregTech.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PipeWireClientModels {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation WIRE_TEX = GregTech.id("block/material_icons/metallic/wire");
    private static final ResourceLocation RUBBER_TEX = GregTech.id("block/material_icons/rubber/pipeside");
    private static final ResourceLocation RESTRICTOR_TEX = GregTech.id("block/iconsets/pipe_restrictor");
    private static final ResourceLocation AXLE_TEX = GregTech.id("block/iconsets/axle");

    private PipeWireClientModels() {}

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        BlockItemModelHelper.captureFromVanillaBlockItem(models);
        int replaced = 0;

        for (var entry : GTFluidPipes.all()) {
            if (!entry.isPresent()) continue;
            FluidPipeBlock block = entry.get();
            PipeSpec spec = block.spec();
            String set = spec.material().getTextureSet().name().toLowerCase(Locale.ROOT);
            double half = Math.max(1.0, spec.diameter() * 8.0);
            ResourceLocation side = GregTech.id("block/material_icons/" + set + "/pipeside");
            ResourceLocation end = GregTech.id("block/material_icons/" + set + "/pipe"
                    + spec.size().name().toLowerCase(Locale.ROOT));
            replaced += replace(models, block,
                    new PipeWireBakedModel(half, side, 0, end, 0, 0, null));
        }

        for (var entry : GTItemPipes.all()) {
            if (!entry.isPresent()) continue;
            ItemPipeBlock block = entry.get();
            ItemPipeSpec spec = block.spec();
            String set = spec.material().getTextureSet().name().toLowerCase(Locale.ROOT);
            double half = Math.max(1.0, spec.diameter() * 8.0);
            String sizeName = spec.size().name().toLowerCase(Locale.ROOT).replace("restrictive_", "");
            ResourceLocation side = GregTech.id("block/material_icons/" + set + "/pipeside");
            ResourceLocation end = GregTech.id("block/material_icons/" + set + "/pipe" + sizeName);
            ResourceLocation overlay = spec.restrictive() ? RESTRICTOR_TEX : null;
            replaced += replace(models, block,
                    new PipeWireBakedModel(half, side, 0, end, 0, 0, overlay));
        }

        for (var entry : GTWires.all()) {
            if (!entry.isPresent()) continue;
            ElectricWireBlock block = entry.get();
            WireSpec spec = block.spec();
            double half = spec.halfThickness();
            // sheath ring on connection faces: 1px for thin cables up to 2px for thick
            double ring = Math.max(1.0, Math.min(2.0, half / 4.0));
            PipeWireBakedModel model = spec.insulated()
                    // rubber sheath body (tint 1), wire core on connection faces (tint 0)
                    ? new PipeWireBakedModel(half, RUBBER_TEX, 1, WIRE_TEX, 0, ring, null)
                    : new PipeWireBakedModel(half, WIRE_TEX, 0, WIRE_TEX, 0, 0, null);
            replaced += replace(models, block, model);
        }

        for (var entry : com.gregtech.gregtech.registry.GTSignalWires.all()) {
            var block = entry.get();
            replaced += replace(models, block, block.insulated()
                    ? new PipeWireBakedModel(2, GregTech.id("block/iconsets/insulation_full"), 1, WIRE_TEX, 0, 1, null)
                    : new PipeWireBakedModel(1, WIRE_TEX, 0, WIRE_TEX, 0, 0, null));
        }

        for (var entry : GTAxles.all()) {
            if (!entry.isPresent()) continue;
            AxleBlock block = entry.get();
            double half = block.spec().halfThickness();
            AxleBakedModel model = new AxleBakedModel(half);
            replaced += replace(models, block, model);
        }

        LOGGER.info("[{}] Dynamic pipe/wire models: {} state variants replaced", GregTech.MODID, replaced);
    }

    private static int replace(Map<ResourceLocation, BakedModel> models, Block block, BakedModel model) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) return 0;
        BlockItemClientModels.aliasItemInventory(models, id, BlockItemClientModels.asBlockItem(model));
        int count = 0;
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            models.put(BlockModelShaper.stateToModelLocation(id, state), model);
            count++;
        }
        return count;
    }
}
