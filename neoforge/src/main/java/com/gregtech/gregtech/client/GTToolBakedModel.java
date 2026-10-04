package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.item.GTToolItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Composites handle + head material layers (or a tinted iconset for headless tools). */
public final class GTToolBakedModel implements BakedModel {
    private final BakedModel base;
    private final ItemStack stack;
    private final ItemOverrides overrides;

    public GTToolBakedModel(BakedModel base) {
        this(base, ItemStack.EMPTY);
    }

    private GTToolBakedModel(BakedModel base, ItemStack stack) {
        this.base = base;
        this.stack = stack;
        this.overrides = new GTToolOverrides(base);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return quadsForStack(stack, state, direction, random, null, ModelData.EMPTY);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random,
                                    ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        ItemStack fromData = data.get(GTToolModel.STACK);
        ItemStack effective = fromData != null && !fromData.isEmpty() ? fromData : stack;
        return quadsForStack(effective, state, direction, random, renderType, data);
    }

    private List<BakedQuad> quadsForStack(ItemStack effective, @Nullable BlockState state, @Nullable Direction direction,
                                          RandomSource random, @Nullable net.minecraft.client.renderer.RenderType renderType,
                                          ModelData data) {
        if (effective.isEmpty() || !(effective.getItem() instanceof GTToolItem)&&!(effective.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem)) {
            return base.getQuads(state, direction, random, data, renderType);
        }
        List<BakedQuad> quads = buildQuads(effective, direction, random, renderType);
        return quads.isEmpty() ? base.getQuads(state, direction, random, data, renderType) : quads;
    }

    private List<BakedQuad> buildQuads(ItemStack stack, @Nullable Direction direction, RandomSource random,
                                       @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if(stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem electric){
            var spec=electric.definition();String body=switch(spec.name()){case "Drill"->"handle_electric_drill";case "Mixer"->"handle_electric_mixer";case "Trimmer"->"handle_electric_trimmer";case "BuzzSaw"->"handle_buzzsaw";default->"power_unit_"+spec.tierName();};
            var out=new ArrayList<BakedQuad>();appendIconsetQuads(out,ToolIconSets.icon(body),0,direction,random,renderType);
            if(java.util.Set.of("Drill","Mixer","Trimmer").contains(spec.name()))appendIconsetQuads(out,ToolIconSets.icon("tip_electric_"+spec.name().toLowerCase(java.util.Locale.ROOT)),1,direction,random,renderType);
            else appendMaterialQuads(out,electric.headMaterial(stack),com.gregtech.gregtech.api.prefix.PrefixRegistry.byName(spec.headPrefix()),1,direction,random,renderType);
            return out;
        }
        GTToolType type = ((GTToolItem)stack.getItem()).toolType();
        GTMaterial head = GTToolHelper.getHead(stack);
        GTMaterial handle = GTToolHelper.getHandle(stack);

        List<BakedQuad> quads = new ArrayList<>();
        if (type == GTToolType.GEM_PICK) {
            appendMaterialQuads(quads, handle, MaterialPrefix.stick, 0, direction, random, renderType);
            appendMaterialQuads(quads, GTToolHelper.gemPickBodyMaterial(), MaterialPrefix.toolHeadPickaxe,
                    ToolIconSets.OVERLAY_TINT, direction, random, renderType);
            appendMaterialQuads(quads, head, MaterialPrefix.toolHeadPickaxeGem, 1, direction, random, renderType);
        } else if (type.requiresHeadAssembly()) {
            MaterialPrefix headPrefix = type.headPrefix();
            if (type.handleIcon() != null) {
                // Custom handle iconsets (sword/file/screwdriver/chisel): head behind, handle in front.
                if (headPrefix != null) {
                    appendMaterialQuads(quads, head, headPrefix, 1, direction, random, renderType);
                }
                appendIconsetQuads(quads, type.handleIcon(), 0, direction, random, renderType);
            } else {
                appendMaterialQuads(quads, handle, MaterialPrefix.stick, 0, direction, random, renderType);
                if (headPrefix != null) {
                    appendMaterialQuads(quads, head, headPrefix, 1, direction, random, renderType);
                }
            }
        } else if (type.headlessIcon() != null) {
            appendIconsetQuads(quads, type.headlessIcon(), 0, direction, random, renderType);
            if(type.definition().isGun())appendIconsetQuads(quads,type.handleIcon(),1,direction,random,renderType);
        }
        return quads;
    }

    private void appendMaterialQuads(List<BakedQuad> out, GTMaterial material, MaterialPrefix prefix, int tintIndex,
                                     @Nullable Direction direction, RandomSource random,
                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
        BakedModel model = GTToolModelResolver.materialModel(material, prefix);
        if (model == null) {
            return;
        }
        for (BakedQuad quad : model.getQuads(null, direction, random, ModelData.EMPTY, renderType)) {
            out.add(retint(quad,quad.getTintIndex()==0?tintIndex:ToolIconSets.OVERLAY_TINT));
        }
    }

    private void appendIconsetQuads(List<BakedQuad> out, ResourceLocation icon, int colorTint,
                                    @Nullable Direction direction, RandomSource random,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        appendIconsetLayer(out, GTToolModel.iconsetModelId(icon), colorTint, direction, random, renderType);
        appendIconsetLayer(out, GTToolModel.iconsetOverlayModelId(icon), ToolIconSets.OVERLAY_TINT, direction, random, renderType);
    }

    private void appendIconsetLayer(List<BakedQuad> out, ResourceLocation modelId, int tintIndex,
                                    @Nullable Direction direction, RandomSource random,
                                    @Nullable net.minecraft.client.renderer.RenderType renderType) {
        BakedModel model = GTToolModelResolver.iconsetModel(modelId);
        if (model == null) {
            return;
        }
        for (BakedQuad quad : model.getQuads(null, direction, random, ModelData.EMPTY, renderType)) {
            out.add(retint(quad, tintIndex));
        }
    }

    private static BakedQuad retint(BakedQuad quad, int tintIndex) {
        if (quad.getTintIndex() == tintIndex) {
            return quad;
        }
        return new BakedQuad(quad.getVertices(), tintIndex, quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    @Override
    public boolean useAmbientOcclusion() {
        return base.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return base.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return base.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return base.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private static final class GTToolOverrides extends ItemOverrides {
        private final BakedModel base;

        GTToolOverrides(BakedModel base) {
            this.base = base;
        }

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            if (!(stack.getItem() instanceof com.gregtech.gregtech.item.ElectricToolItem) && (!(stack.getItem() instanceof GTToolItem) || !GTToolHelper.isTool(stack))) {
                return base;
            }
            return new GTToolBakedModel(base, stack);
        }
    }
}
