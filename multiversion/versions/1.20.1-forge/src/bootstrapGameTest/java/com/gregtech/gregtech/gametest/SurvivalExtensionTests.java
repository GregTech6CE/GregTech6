package com.gregtech.gregtech.gametest;
import com.gregtech.gregtech.api.material.*;
import com.gregtech.gregtech.api.recipe.*;
import com.gregtech.gregtech.data.*;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("gregtech") @PrefixGameTestTemplate(false)
public final class SurvivalExtensionTests {
 @GameTest(template="test_empty") public static void unitSearchAliasesIncludeVanillaIron(GameTestHelper h) {
  var unit=GTItems.getStack(MaterialPrefix.unit,GTMaterialRegistry.get("Iron"));
  var aliases=MaterialDisplayBinding.alternatives(unit);
  h.assertTrue(aliases.stream().anyMatch(s->s.is(Items.IRON_INGOT)),"Unit iron indexes vanilla iron ingot");
  h.assertTrue(aliases.stream().noneMatch(s->s.is(Items.GOLD_INGOT)),"Does not alias another material");
  h.assertTrue(MaterialDisplayBinding.alternatives(new ItemStack(Items.IRON_INGOT)).isEmpty(),"Physical recipes retain exact ingredients");h.succeed();
 }
 @GameTest(template="test_empty") public static void stoneAnvilAndToolRoutesAreReachable(GameTestHelper h) {
  for(String path:List.of("stone_granite_black_stone","stone_granite_black_cobble","block_ore_bauxite")) {
   var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech",path));
   h.assertTrue(item!=null&&item!=Items.AIR,"Real worldgen block "+path);
   h.assertTrue(MachineRecipeMaps.Crusher.findRecipe(List.of(new ItemStack(item)),List.of(),false,12,12)!=null,"Crusher handles "+path);
  }
  var material=GTMaterialRegistry.get("Iron");
  var raw=GTItems.getStack(MaterialPrefix.toolHeadRawPickaxe,material);
  h.assertTrue(MachineRecipeMaps.Sharpening.findRecipe(List.of(raw),List.of(),false,12,12)!=null,"Cast pickaxe head can be finished");
  var rock=GTItems.getStack(MaterialPrefix.rockGt,GTMaterialRegistry.get("GraniteBlack"));
  h.assertTrue(MachineRecipeMaps.Anvil.findRecipe(Arrays.asList(rock,ItemStack.EMPTY),List.of(),false,12,12)!=null,"Granite rock has anvil crushing");
  var stoneRecipes=h.getLevel().getRecipeManager().getRecipes().stream().filter(r->r.getId().getNamespace().equals("gregtech")&&r.getId().getPath().startsWith("stone_survival/")).toList();
  h.assertTrue(stoneRecipes.size()==81,"81 stone furnace/crafting recipes loaded");
  for(var recipe:stoneRecipes) {
   h.assertTrue(!recipe.getResultItem(h.getLevel().registryAccess()).isEmpty(),"Stone recipe result");
   for(var ingredient:recipe.getIngredients())h.assertTrue(ingredient.getItems().length>0,"Stone recipe ingredient");
  }
  h.succeed();
 }
 @GameTest(template="test_empty") public static void normalOreDropsRawAndSilkPreservesHost(GameTestHelper h) {
  var ore=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValues().stream().filter(b->b instanceof com.gregtech.gregtech.block.OreBlock o&&!o.isSmall()&&o.material()==GTMaterialRegistry.get("Iron")).map(b->(com.gregtech.gregtech.block.OreBlock)b).findFirst().orElseThrow();
  var pos=h.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
  // §103.B: the host rock is the ore's `stone` block-state property (it was the ore block entity).
  var state=ore.stateFor(com.gregtech.gregtech.block.OreHostStone.GRANITE_BLACK);
  h.getLevel().setBlock(pos,state,3);
  h.assertTrue(h.getLevel().getBlockEntity(pos)==null,"§103.B: an ore block keeps no block entity");
  h.assertTrue(com.gregtech.gregtech.block.OreBlock.stoneOf(h.getLevel().getBlockState(pos))==com.gregtech.gregtech.block.OreHostStone.GRANITE_BLACK,
          "the placed state keeps the host rock");
  var raw=GTItems.getStack(MaterialPrefix.oreRaw,GTMaterialRegistry.get("Iron"));
  h.assertTrue(!raw.isEmpty(),"Iron raw ore form must be registered");
  var pick=new ItemStack(Items.DIAMOND_PICKAXE);
  var drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),pos,null,null,pick);
  h.assertTrue(drops.size()==1&&drops.get(0).is(raw.getItem())&&drops.get(0).getCount()==1,"Ordinary ore gives one raw ore");
  pick.enchant(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH,1);
  drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),pos,null,null,pick);
  h.assertTrue(drops.size()==1&&drops.get(0).is(ore.asItem()),"Silk gives ore block");
  h.assertTrue(drops.get(0).getTagElement("BlockEntityTag")!=null,"Silk keeps host stone NBT");
  h.assertTrue("stone_granite_black".equals(drops.get(0).getTagElement("BlockStateTag").getString("stone")),
          "§103.B: silk also writes the vanilla BlockStateTag");
  h.assertTrue(com.gregtech.gregtech.block.OreBlock.stoneOfStack(drops.get(0))==com.gregtech.gregtech.block.OreHostStone.GRANITE_BLACK,
          "the dropped item reads back as its host rock");
  for(var b:net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValues())if(b instanceof com.gregtech.gregtech.block.OreBlock o&&!o.isSmall())
   h.assertTrue(!GTItems.getStack(MaterialPrefix.oreRaw,o.material()).isEmpty(),"Every normal ore has a registered raw drop: "+o.material().getName());
  h.succeed();
 }
}
