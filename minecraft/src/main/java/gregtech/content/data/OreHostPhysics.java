package gregtech.content.data;

import gregtech.api.material.*;

import gregtech.engine.addon.AddonMaterials;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.engine.prefix.BlockMaterialPrefix;
import gregtech.content.block.OreBlock;
import gregtech.content.registry.GTBlocks;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Reviewed shared native owner/state adapter. No new blocks, tags, or entity state. */
public final class OreHostPhysics {
 private OreHostPhysics() {}
 public static Optional<SourceOreHostPhysics.Params> params(BlockState state) {
  if(state==null || state.getBlock().getClass()!=OreBlock.class) return Optional.empty();
  OreBlock block=(OreBlock)state.getBlock();
  var prefix=block.prefix();
  if(prefix!=BlockMaterialPrefix.ore && prefix!=BlockMaterialPrefix.oreSmall) return Optional.empty();
  var material=block.material();
  if(material.resolve()!=material || !GTMaterialRegistry.isCoreMaterial(material)
      || AddonMaterials.owns(material)) return Optional.empty();
  // Explosive resistance special rules need separately reviewed material provenance.
  if(material.has(gregtech.api.material.MaterialProperty.EXPLOSIVE))return Optional.empty();
  if(!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("gt6ce")) return Optional.empty();
  var canonical=GTBlocks.getObject(prefix,material);
//? if >=1.21.1 {
/*  if(canonical==null || !canonical.isBound() || canonical.get()!=block) return Optional.empty();
*///?} else {
  if(canonical==null || !canonical.isPresent() || canonical.get()!=block) return Optional.empty();
//?}

  boolean small=prefix==BlockMaterialPrefix.oreSmall,broken=OreBlock.isBroken(state);
  if(small&&broken) return Optional.empty(); // no source equivalent proven for this synthetic combination
  var mode=small?SourceOreHostPhysics.Mode.SMALL:broken?SourceOreHostPhysics.Mode.BROKEN:SourceOreHostPhysics.Mode.NORMAL;
  return SourceOreHostPhysics.lookup(OreBlock.stoneOf(state).id(),mode,material.getToolQuality());
 }
 public static Optional<SourceOreHostPhysics.Params> itemParams(ItemStack stack) {
  if(stack.isEmpty() || !(stack.getItem() instanceof BlockItem item) || item.getBlock().getClass()!=OreBlock.class)
   return Optional.empty();
  OreBlock block=(OreBlock)item.getBlock();
  String raw=rawHost(stack);
  if(raw!=null && java.util.Arrays.stream(gregtech.content.block.OreHostStone.values()).noneMatch(h->h.id().equals(raw)))
   return Optional.empty(); // serialized unknown host must not become source STONE through byId fallback
  return params(block.stateFor(OreBlock.stoneOfStack(stack)).setValue(OreBlock.BROKEN,OreBlock.isBrokenStack(stack)));
 }
 private static String rawHost(ItemStack stack) {
//? if >=1.21.1 {
/*  var properties=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_STATE,
       net.minecraft.world.item.component.BlockItemStateProperties.EMPTY).properties();
  if(properties.containsKey(OreBlock.STONE.getName()))return properties.get(OreBlock.STONE.getName());
  var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
  if(data!=null){var tag=data.copyTag();var state=tag.getCompound("BlockStateTag");
   if(state.contains("stone"))return state.getString("stone");
   var entity=tag.getCompound("BlockEntityTag");if(entity.contains("stone"))return entity.getString("stone");}
*///?} else {
  var state=stack.getTagElement("BlockStateTag");if(state!=null&&state.contains("stone"))return state.getString("stone");
  var entity=stack.getTagElement("BlockEntityTag");if(entity!=null&&entity.contains("stone"))return entity.getString("stone");
//?}
  return null;
 }

}
