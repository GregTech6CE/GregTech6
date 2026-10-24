package gregtech.gametest;

import gregtech.internal.material.GTMaterialRegistry;
import gregtech.internal.tool.GTToolHelper;
import gregtech.internal.tool.GTToolType;
import gregtech.block.misc.SpikeBlock;
import gregtech.item.GTToolItem;
import gregtech.registry.GTSpikes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.*;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Existing production APIs only: suitable before the state-level fix. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class SpikeHarvestStateTests {
    private static final Map<SpikeBlock.Family, List<String>> MATERIALS = Map.of(
            SpikeBlock.Family.METAL,List.of("Copper","Lead"), SpikeBlock.Family.STEEL,List.of("BlueSteel","RedSteel"),
            SpikeBlock.Family.SHARP,List.of("Steel","Titanium"), SpikeBlock.Family.FANCY,List.of("Gold","Silver"),
            SpikeBlock.Family.SUPER,List.of("TungstenSteel","Adamantium"));
    private static final Map<String,Integer> SOURCE_QUALITY = Map.of(
            "Copper",0,"Lead",1,"Steel",2,"Titanium",3,"BlueSteel",2,"RedSteel",2,
            "Gold",2,"Silver",2,"TungstenSteel",4,"Adamantium",5);
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
//? if >=1.21.1 {
        /*var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
        var p = net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "SpikeHarvestAudit"));
        p.setGameMode(GameType.SURVIVAL);return p;
    }
    private static boolean harvest(GameTestHelper h, net.minecraft.server.level.ServerPlayer p, BlockState state, BlockPos pos) {
//? if >=1.21.1 {
        /*return p.hasCorrectToolForDrops(state,h.getLevel(),pos);
*///?} else {
        return p.hasCorrectToolForDrops(state);
//?}
    }
    /** Readiness retries do not mutate the fixture; the body executes exactly once. */
    private static void whenReady(GameTestHelper h, Runnable body) {
        var owned = new HashSet<BlockPos>();
        h.forEveryBlockInStructure(at -> owned.add(h.absolutePos(at)));
        h.startSequence().thenWaitUntil(() -> {
            for (var local : BlockPos.betweenClosed(new BlockPos(1,1,1),new BlockPos(5,5,5))) {
                var at=h.absolutePos(local);
                h.assertTrue(owned.contains(at), "harvest fixture stays inside dedicated template");
                h.assertTrue(h.getLevel().areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))
                        && h.getLevel().isPositionEntityTicking(at), "waiting for entity-loaded/ticking harvest fixture");
            }
        }).thenExecute(body);
    }
    private static void cleanup(GameTestHelper h, BlockPos pos, BlockState before,
                                net.minecraft.server.level.ServerPlayer player) {
        h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(.8),
                e -> e.getItem().getItem() instanceof BlockItem bi && bi.getBlock() instanceof SpikeBlock)
                .forEach(ItemEntity::discard);
        h.getLevel().getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(pos));
        h.getLevel().setBlock(pos,before,3);
        player.discard();
    }
    @GameTest(template="spike_harvest_empty",timeoutTicks=240)
    public static void sourceMaterialQualityAndNativeVanillaToolsFollowSecondaryState(GameTestHelper h) {
        whenReady(h, () -> run_sourceMaterialQualityAndNativeVanillaToolsFollowSecondaryState(h));
    }
    private static void run_sourceMaterialQualityAndNativeVanillaToolsFollowSecondaryState(GameTestHelper h) {
        var p=player(h);var pos=h.absolutePos(new BlockPos(3,3,3));int cases=0;
        var before=h.getLevel().getBlockState(pos);
        p.setPos(pos.getX()-1,pos.getY(),pos.getZ()-1);
        try {
        Item[] picks={Items.WOODEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.NETHERITE_PICKAXE};
        for(var registered:GTSpikes.all())for(int secondary=0;secondary<2;secondary++)for(var mode:SpikeBlock.Mode.values()) {
            var block=registered.get();var state=block.defaultBlockState().setValue(SpikeBlock.SECONDARY,secondary==1).setValue(SpikeBlock.MODE,mode);
            String material=MATERIALS.get(block.family()).get(secondary);
            int required=SOURCE_QUALITY.get(material);
            h.assertTrue(GTMaterialRegistry.get(material).getToolQuality()==required,"live material preserves audited MT.java quality "+material);
            h.assertTrue(GTToolHelper.requiredHarvestLevel(state)==required,"source state tier "+block.family()+"/"+secondary+"/"+mode);
            h.getLevel().setBlockAndUpdate(pos,state);
            for(int tier=0;tier<picks.length;tier++){
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(picks[tier]));
                h.assertTrue(harvest(h,p,state,pos)==(tier>=required),"native pick tier "+tier+" vs source "+required);cases++;
            }
            for(var wrong:List.of(ItemStack.EMPTY,new ItemStack(Items.NETHERITE_AXE))){
                p.setItemInHand(InteractionHand.MAIN_HAND,wrong);
                h.assertTrue(!harvest(h,p,state,pos),"wrong tool cannot bypass Spike quality");
            }
        }
        h.assertTrue(cases==150,"five families two materials three modes five vanilla tiers");h.succeed();
        } finally { cleanup(h,pos,before,p); }
    }
    @GameTest(template="spike_harvest_empty",timeoutTicks=240)
    public static void actualSurvivalBreakRejectsLowTierAndKeepsSecondaryDrop(GameTestHelper h) {
        whenReady(h, () -> run_actualSurvivalBreakRejectsLowTierAndKeepsSecondaryDrop(h));
    }
    private static void run_actualSurvivalBreakRejectsLowTierAndKeepsSecondaryDrop(GameTestHelper h) {
        var p=player(h);var block=GTSpikes.get(SpikeBlock.Family.SHARP);
        var pos=h.absolutePos(new BlockPos(3,3,3));
        var before=h.getLevel().getBlockState(pos);
        p.setPos(pos.getX()-1,pos.getY(),pos.getZ()-1);
        try {
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(.8)).isEmpty(),"dedicated drop fixture starts without items");
        var state=block.defaultBlockState().setValue(SpikeBlock.SECONDARY,true).setValue(SpikeBlock.MODE,SpikeBlock.Mode.OMNI);
        int required=GTMaterialRegistry.get("Titanium").getToolQuality();h.assertTrue(required>0,"source titanium requires tier above wood");
        h.getLevel().setBlockAndUpdate(pos,state);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WOODEN_PICKAXE));
        h.assertTrue(!harvest(h,p,state,pos),"wood cannot harvest titanium");
        h.assertTrue(p.gameMode.destroyBlock(pos),"survival destruction occurs even without eligible drops");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(.8),e->e.getItem().is(block.asItem())).isEmpty(),"under-tier actual destruction no spike drop");
        h.getLevel().setBlockAndUpdate(pos,state);
        var high=GTToolItem.create(GTToolType.PICKAXE,GTMaterialRegistry.get("Iridium"),GTMaterialRegistry.get("Wood"));
        h.assertTrue(GTToolHelper.getHarvestLevel(high)>=required,"registered GT pick fixture meets titanium quality");
        p.setItemInHand(InteractionHand.MAIN_HAND,high);h.assertTrue(harvest(h,p,state,pos),"GT pick can harvest state");
        h.assertTrue(p.gameMode.destroyBlock(pos),"real correct-tool destruction");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(.8),e->e.getItem().is(block.asItem()));
        h.assertTrue(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1,"one native titanium spike drop");
        h.assertTrue(drops.stream().allMatch(e->SpikeBlock.itemSecondary(e.getItem())&&SpikeBlock.itemMode(e.getItem())==SpikeBlock.Mode.OMNI),"drop retains original material and mode");
        drops.forEach(ItemEntity::discard);h.succeed();
        } finally { cleanup(h,pos,before,p); }
    }
}
