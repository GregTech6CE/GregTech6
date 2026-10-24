package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.prefix.BlockMaterialPrefix;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.client.CommonBlockTooltips;
import gregtech.content.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//? if >=1.21.1 {
/*@net.neoforged.neoforge.gametest.GameTestHolder("gregtech_ore_source_audit")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
*///?} else {
@net.minecraftforge.gametest.GameTestHolder("gregtech_ore_source_audit")
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
//?}
/** API-neutral native boundaries; no candidate policy supplies the expected values. */
public final class OreHostHarvestTests {
    @GameTest(template="ore_host_physics_owned",batch="ore_host_physics",timeoutTicks=100)
    public static void nativePlayerHarvestHonorsSmallBrokenOffsetsAndHostMinimum(GameTestHelper h) {
        OreHostPhysicsFixture.run(h, players -> nativePlayerHarvestHonorsSmallBrokenOffsetsAndHostMinimumBody(h, players));
    }
    private static void nativePlayerHarvestHonorsSmallBrokenOffsetsAndHostMinimumBody(GameTestHelper h, java.util.List<net.minecraft.server.level.ServerPlayer> players) {
        var player=player(h,players); var pos=OreHostPhysicsFixture.templatePos(h,new BlockPos(3,3,3));
        var normal=state(h,false,false,OreHostStone.STONE);
        var small=state(h,true,false,OreHostStone.STONE);
        var broken=state(h,false,true,OreHostStone.STONE);
        // Check the real state-level requirement before tool/category assertions can mask it.
        h.assertTrue(!state(h,false,false,OreHostStone.SAND).requiresCorrectToolForDrops()
                && !state(h,true,false,OreHostStone.RED_SAND).requiresCorrectToolForDrops()
                && !state(h,false,false,OreHostStone.GRAVEL).requiresCorrectToolForDrops(),
                "source Material.sand state does not require a correct tool for drops");
        h.assertTrue(normal.requiresCorrectToolForDrops()&&small.requiresCorrectToolForDrops()&&broken.requiresCorrectToolForDrops(),
                "source rock state still requires a correct tool for drops");

        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_PICKAXE));
        h.assertTrue(!correct(h,player,normal,pos),"quality2 normal requires iron tier, stone pick rejected");
        h.assertTrue(correct(h,player,small,pos),"small offset-1 makes quality2 require stone tier");
        h.assertTrue(correct(h,player,broken,pos),"broken offset-1 makes quality2 require stone tier");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(correct(h,player,normal,pos),"iron pick accepts quality2 normal");
        h.assertTrue(!correct(h,player,state(h,false,false,OreHostStone.GRANITE_BLACK),pos),
                "GT black granite host minimum3 rejects iron pick despite material quality2");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
        h.assertTrue(correct(h,player,state(h,false,false,OreHostStone.GRANITE_BLACK),pos),
                "diamond pick satisfies GT black granite host minimum3");
        for(var wrong:new ItemStack[]{ItemStack.EMPTY,new ItemStack(Items.DIAMOND_SHOVEL),new ItemStack(Items.DIAMOND_AXE)}) {
            player.setItemInHand(InteractionHand.MAIN_HAND,wrong);
            for(var s:new BlockState[]{normal,small,broken})
                h.assertTrue(!correct(h,player,s,pos),"high-tier wrong tool kind or empty hand cannot harvest rock ore");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_SHOVEL));
        h.assertTrue(correct(h,player,state(h,true,false,OreHostStone.SAND),pos),"preferred stone shovel retains drop permission on small sand host");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
        h.assertTrue(correct(h,player,state(h,true,false,OreHostStone.SAND),pos),"source Material.sand permits drops even with a wrong tool category");
        for(var host:new OreHostStone[]{OreHostStone.SAND,OreHostStone.RED_SAND,OreHostStone.GRAVEL})
            for(boolean isSmall:new boolean[]{false,true})
                for(var held:new ItemStack[]{ItemStack.EMPTY,new ItemStack(Items.WOODEN_PICKAXE),new ItemStack(Items.WOODEN_SHOVEL),new ItemStack(Items.DIAMOND_AXE)}) {
                    player.setItemInHand(InteractionHand.MAIN_HAND,held);
                    h.assertTrue(correct(h,player,state(h,isSmall,false,host),pos),
                            "source Material.sand permits hand/wrong-category/below-tier drops: "+host+" small="+isSmall);
                }
        var sand=state(h,true,false,OreHostStone.SAND);
        var gtPick=gregtech.content.item.GTToolItem.create(gregtech.internal.tool.GTToolType.PICKAXE,
                GTMaterialRegistry.get("Steel").resolve(),GTMaterialRegistry.get("Wood").resolve());
        h.assertTrue(!gtPick.isEmpty()&&gregtech.internal.tool.GTToolHelper.isUsable(gtPick),"real assembled GT pick fixture");
        player.setItemInHand(InteractionHand.MAIN_HAND,gtPick);
        h.assertTrue(correct(h,player,sand,pos),"source hand-harvestable host permits GT wrong-tool drops");
        h.assertTrue(!gregtech.internal.tool.GTToolHelper.canEffectiveHarvest(gtPick,sand),
                "drop permission does not make wrong-category GT pick effective for sand mining speed");

        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        player.setOnGround(true);player.removeAllEffects();h.getLevel().setBlockAndUpdate(pos,sand);
        near(h,sand.getDestroyProgress(player,h.getLevel(),pos),1F/30F,
                "small sand source hardness1 permits hand progress without wrong-tool100 divisor");
        near(h,new ItemStack(Items.STONE_SHOVEL).getDestroySpeed(sand),4F,"proper vanilla shovel uses its literal tier speed on sand host");
        near(h,new ItemStack(Items.DIAMOND_PICKAXE).getDestroySpeed(sand),1F,"wrong vanilla pick has base speed on sand host, not static pickaxe-tag speed");
        near(h,new ItemStack(Items.DIAMOND_SHOVEL).getDestroySpeed(normal),1F,"wrong shovel has base speed on stone host");
//? if >=1.21.1 {
        /*var custom=new ItemStack(Items.DIAMOND_PICKAXE);
        custom.set(net.minecraft.core.component.DataComponents.TOOL,
                new net.minecraft.world.item.component.Tool(List.of(),7F,1));
        near(h,custom.getDestroySpeed(sand),7F,"public custom TOOL component retains its own speed instead of ore category coercion");
        player.setItemInHand(InteractionHand.MAIN_HAND,custom);
        h.assertTrue(!correct(h,player,normal,pos),"custom TOOL with no correct-drop rules is not force-promoted by diamond identity");
*///?}
        var drillItem=gregtech.content.registry.GTElectricItems.get("electric_mining_drill");
        var steel=GTMaterialRegistry.get("Steel").resolve();
        h.assertTrue(steel.getToolQuality()==2&&drillItem.definition().quality()==0,"existing LV drill quality2 fixture");
        var drill=drillItem.assembled(steel,10000);
        h.assertTrue(!drill.isEmpty(),"actual public assembled drill");
        player.setItemInHand(InteractionHand.MAIN_HAND,drill);
        h.assertTrue(!correct(h,player,normal,pos),"uncharged drill cannot harvest");
        h.assertTrue(drillItem.doEnergyInjection(gregtech.content.data.GregTechTags.Energy.EU,
                drill,32,32,h.getLevel(),pos,true)==32,"actual public LV charging accepts32 packets");
        h.assertTrue(drillItem.isPoweredUsable(drill),"charged drill is usable");
        h.assertTrue(correct(h,player,normal,pos)&&correct(h,player,small,pos)&&correct(h,player,broken,pos),
                "powered qualifying drill retains ore harvesting");
        h.assertTrue(!correct(h,player,state(h,false,false,OreHostStone.GRANITE_BLACK),pos),
                "powered drill does not bypass GT host minimum3");

    }

    @GameTest(template="ore_host_physics_owned",batch="ore_host_physics",timeoutTicks=100)
    public static void nativeDestroyProgressUsesHardnessOnceWithoutLegacyBrokenMultiplier(GameTestHelper h) {
        OreHostPhysicsFixture.run(h, players -> nativeDestroyProgressUsesHardnessOnceWithoutLegacyBrokenMultiplierBody(h, players));
    }
    private static void nativeDestroyProgressUsesHardnessOnceWithoutLegacyBrokenMultiplierBody(GameTestHelper h, java.util.List<net.minecraft.server.level.ServerPlayer> players) {
        var player=player(h,players);var pos=OreHostPhysicsFixture.templatePos(h,new BlockPos(3,3,3));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        player.setOnGround(true); player.removeAllEffects();
        var normal=state(h,false,false,OreHostStone.STONE);
        var small=state(h,true,false,OreHostStone.STONE);
        var broken=state(h,false,true,OreHostStone.STONE);
        h.assertTrue(correct(h,player,normal,pos)&&correct(h,player,small,pos)&&correct(h,player,broken,pos),
                "same unenchanted iron pick qualifies for all three, removing harvest penalty ambiguity");
        h.getLevel().setBlockAndUpdate(pos,normal);
        float n=normal.getDestroyProgress(player,h.getLevel(),pos);
        h.getLevel().setBlockAndUpdate(pos,small);
        float s=small.getDestroyProgress(player,h.getLevel(),pos);
        h.getLevel().setBlockAndUpdate(pos,broken);
        float b=broken.getDestroyProgress(player,h.getLevel(),pos);
        h.assertTrue(Float.isFinite(n)&&n>0&&Float.isFinite(s)&&s>0&&Float.isFinite(b)&&b>0,"real native progress is finite positive");
        near(h,s/n,1.5F,"source hardness3/2 yields small progress1.5x");
        near(h,b/n,3F,"source hardness3/1 yields broken progress3x, not extra legacy x2");

    }

    @GameTest(template="ore_host_physics_owned",batch="ore_host_physics",timeoutTicks=100)
    public static void commonItemTooltipReadsHostBrokenStateAndHidesLowTier(GameTestHelper h) {
        OreHostPhysicsFixture.run(h, players -> commonItemTooltipReadsHostBrokenStateAndHidesLowTierBody(h, players));
    }
    private static void commonItemTooltipReadsHostBrokenStateAndHidesLowTierBody(GameTestHelper h, java.util.List<net.minecraft.server.level.ServerPlayer> players) {
        tooltip(h,state(h,false,false,OreHostStone.STONE),2,"iron",null);
        tooltip(h,state(h,true,false,OreHostStone.STONE),1,null,null);
        tooltip(h,state(h,false,true,OreHostStone.STONE),1,null,null);
        tooltip(h,state(h,false,false,OreHostStone.GRANITE_BLACK),3,"diamond","24.0");
        tooltip(h,state(h,false,true,OreHostStone.GRANITE_BLACK),2,"iron","9.0");
        var sedimentStack=OreBlock.withStone(new ItemStack(state(h,false,false,OreHostStone.SAND).getBlock()),
                state(h,false,false,OreHostStone.SAND));
        var sedimentLines=new ArrayList<Component>();sedimentLines.add(Component.literal("Sediment fixture"));
        CommonBlockTooltips.append(sedimentStack,sedimentLines);
        var handRows=sedimentLines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),
                "tooltip.gt6ce.ore.hand_harvest_preferred")).toList();
        h.assertTrue(!CommonBlockTooltips.containsKey(sedimentLines,"gt.lang.tool.to.harvest")&&handRows.size()==1,
                "localized drop-policy tooltip has no mandatory tool or tier");
        var handContent=(net.minecraft.network.chat.contents.TranslatableContents)handRows.get(0).getContents();
        h.assertTrue(handContent.getArgs().length==1&&handContent.getArgs()[0] instanceof Component preferred
                && CommonBlockTooltips.containsKey(List.of(preferred),"gt.lang.tool.name.shovel"),
                "localized optional faster tool argument is shovel");
        CommonBlockTooltips.append(sedimentStack,sedimentLines);
        h.assertTrue(sedimentLines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),
                "tooltip.gt6ce.ore.hand_harvest_preferred")).count()==1,"sediment tooltip remains idempotent");
        var foreignHost=new ItemStack(state(h,false,false,OreHostStone.STONE).getBlock());
//? if >=1.21.1 {
        /*foreignHost.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                new net.minecraft.world.item.component.BlockItemStateProperties(java.util.Map.of("stone","unreviewed:foreign_host")));
*///?} else {
        foreignHost.getOrCreateTagElement("BlockStateTag").putString("stone","unreviewed:foreign_host");
//?}
        var foreignLines=new ArrayList<Component>();foreignLines.add(Component.literal("Unknown host fixture"));
        CommonBlockTooltips.append(foreignHost,foreignLines);
        var foreignBlast=foreignLines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.blastresistance")).toList();
        h.assertTrue(foreignBlast.size()==1&&foreignBlast.get(0).getString().contains("9.0"),
                "explicit unknown serialized host preserves legacyblast9, not defaultSTONE source3 suppression");
        var foreignHarvest=foreignLines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.tool.to.harvest")).toList();
        h.assertTrue(foreignHarvest.size()==1&&foreignHarvest.get(0).getString().contains(" (2,")
                &&CommonBlockTooltips.containsKey(foreignHarvest,"material.gt6ce.iron"),"unknown host retains existing blocklevel2 tooltip");

    }
    private static void tooltip(GameTestHelper h,BlockState state,int tier,String tierMaterial,String blastNumber) {
        var stack=OreBlock.withStone(new ItemStack(state.getBlock()),state);
        h.assertTrue(OreBlock.stoneOfStack(stack)==state.getValue(OreBlock.STONE)
                &&OreBlock.isBrokenStack(stack)==state.getValue(OreBlock.BROKEN),"actual item host/broken state round trip");
        var lines=new ArrayList<Component>();lines.add(Component.literal("Source ore fixture"));
        CommonBlockTooltips.append(stack,lines);
        var harvest=lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.tool.to.harvest")).toList();
        h.assertTrue(harvest.size()==1,"one common harvest row");
        h.assertTrue(CommonBlockTooltips.containsKey(harvest,"gt.lang.tool.name.pickaxe"),"actual common pickaxe label");
        String text=harvest.get(0).getString();
        if(tier>1) {
            h.assertTrue(text.contains(" ("+tier+","),"literal visible source tier "+tier+": "+text);
            h.assertTrue(CommonBlockTooltips.containsKey(harvest,"material.gt6ce."+tierMaterial),"literal tier material translation key");
        } else h.assertTrue(!text.contains(" ("),"source lower tier1 details hidden");
        var blasts=lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.blastresistance")).toList();
        if(blastNumber==null)h.assertTrue(blasts.isEmpty(),"source resistance below4 has no common blast row");
        else h.assertTrue(blasts.size()==1&&blasts.get(0).getString().contains(blastNumber),"item host-aware blast literal "+blastNumber);
        CommonBlockTooltips.append(stack,lines);
        h.assertTrue(lines.stream().filter(c->CommonBlockTooltips.containsKey(List.of(c),"gt.lang.tool.to.harvest")).count()==1,
                "repeat common append does not duplicate harvest rows");
    }
    private static BlockState state(GameTestHelper h,boolean small,boolean broken,OreHostStone host) {
        var material=GTMaterialRegistry.get("Iron").resolve();
        h.assertTrue(material.isValid()&&material.getId()==260&&material.getToolQuality()==2,"existing canonical Iron quality2 fixture");
        var item=GTBlocks.getStack(small?BlockMaterialPrefix.oreSmall:BlockMaterialPrefix.ore,material);
        h.assertTrue(!item.isEmpty()&&item.getItem() instanceof BlockItem,"actual native ore item");
        var block=((BlockItem)item.getItem()).getBlock();
        h.assertTrue(block.getClass()==OreBlock.class,"exact OreBlock owner");
        return ((OreBlock)block).stateFor(host).setValue(OreBlock.BROKEN,broken);
    }
    private static boolean correct(GameTestHelper h,net.minecraft.server.level.ServerPlayer player,BlockState state,BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos,state);
//? if >=1.21.1 {
        /*return player.hasCorrectToolForDrops(state,h.getLevel(),pos);
*///?} else {
        return player.hasCorrectToolForDrops(state);
//?}
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h, java.util.List<net.minecraft.server.level.ServerPlayer> players) {
//? if >=1.21.1 {
        /*var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GTOreHostHarvest"));
        player.setGameMode(GameType.SURVIVAL); players.add(player);
        var p=OreHostPhysicsFixture.templatePos(h,new BlockPos(4,2,4)); player.setPos(p.getX()+0.5,p.getY(),p.getZ()+0.5); return player;
    }
    private static void near(GameTestHelper h,float actual,float expected,String message) {
        h.assertTrue(Math.abs(actual-expected)<0.00001F,message+": expected "+expected+", actual "+actual);
    }
}
