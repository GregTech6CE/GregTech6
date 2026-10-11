package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.tool.DynamiteBlock;
import com.gregtech.gregtech.content.tool.DynamiteSubstrates;
import com.gregtech.gregtech.item.behavior.BehaviorRemote;
import com.gregtech.gregtech.item.behavior.ItemBehaviors;
import com.gregtech.gregtech.registry.GTElectricItems;
import com.gregtech.gregtech.registry.GTToolBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_repair")
@PrefixGameTestTemplate(false)
public final class DrillDynamiteTests {
    private static final BlockPos SUPPORT = new BlockPos(2,2,2);
    private static Player player(GameTestHelper h, boolean creative) {
        var player = new Player(h.getLevel(), BlockPos.ZERO, 0,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "drill-test")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return creative; }
        };
        player.getAbilities().instabuild = creative;
        player.getAbilities().mayBuild = true;
        var drill = new ItemStack(GTElectricItems.ELECTRIC_DRILL.get());
        drill.getOrCreateTag().putLong("gt.charge", 1000);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.getInventory().items.set(35, new ItemStack(GTToolBlocks.DYNAMITE.get(), 3));
        return player;
    }
    private static boolean place(GameTestHelper h, Player player, Direction face) {
        var support = h.absolutePos(SUPPORT);
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)), face, support, false);
        var drill = player.getMainHandItem();
        return drill.getItem().onItemUseFirst(drill, new UseOnContext(h.getLevel(), player,
                InteractionHand.MAIN_HAND, drill, hit)).consumesAction();
    }
    @GameTest(template="test_empty")
    public static void survivalDrillUsesLastChargeAndBindsFirstAvailableHotbarRemote(GameTestHelper h) {
        h.setBlock(SUPPORT, Blocks.DIRT);
        var player = player(h, false);
        var full = ItemBehaviors.stack("gregtech:remote_activator");
        var coords = new java.util.ArrayList<BlockPos>();
        for (int i=0;i<64;i++) coords.add(new BlockPos(i,0,0));
        BehaviorRemote.setCoords(full.getOrCreateTag(), h.getLevel(), coords);
        var remote = ItemBehaviors.stack("gregtech:remote_activator");
        player.getInventory().items.set(1,full); player.getInventory().items.set(2,remote);
        player.getInventory().items.set(10,new ItemStack(GTToolBlocks.BOOMSTICK.get(),2));
        var charge = player.getInventory().items.get(35);
        charge.getOrCreateTag().putString("test", "preserved");
        h.assertTrue(place(h,player,Direction.EAST), "powered drill places a charge on dirt");
        var target=h.absolutePos(SUPPORT.east()); var state=h.getLevel().getBlockState(target);
        h.assertTrue(state.is(GTToolBlocks.DYNAMITE.get()) && state.getValue(DynamiteBlock.SUNK)
                && state.getValue(DynamiteBlock.FACING)==Direction.EAST,"reverse inventory priority and embedded facing");
        h.assertTrue(charge.getCount()==2 && charge.getTag().getString("test").equals("preserved")
                && !charge.getTag().contains("BlockStateTag"),"consume one without leaking temporary placement NBT");
        h.assertTrue(player.getMainHandItem().getTag().getLong("gt.charge")==900,"drilling consumes 100 EU");
        h.assertTrue(BehaviorRemote.getCoords(remote.getTag(),h.getLevel()).equals(java.util.List.of(target)),"full remote skipped for next hotbar remote");
        ((DynamiteBlock)state.getBlock()).refreshSupport(h.getLevel(),target);
        h.assertTrue(h.getLevel().getBlockState(target).getValue(DynamiteBlock.SUNK),"soil remains valid after support refresh");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void creativeDrillPlacesWithoutPowerOrItemConsumption(GameTestHelper h) {
        h.setBlock(SUPPORT,Blocks.COBBLESTONE); var player=player(h,true);
        player.getMainHandItem().getOrCreateTag().putLong("gt.charge",0);
        h.assertTrue(place(h,player,Direction.UP),"creative drill places without energy");
        h.assertTrue(player.getInventory().items.get(35).getCount()==3
                && player.getMainHandItem().getTag().getLong("gt.charge")==0,"creative consumes neither charge nor EU");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void drillRefusesUnbreakableProcessedOccupiedAndUnpoweredTargets(GameTestHelper h) {
        var player=player(h,false);
        for(var block:java.util.List.of(Blocks.BEDROCK,Blocks.STONE_BRICKS,Blocks.OAK_PLANKS)) {
            h.setBlock(SUPPORT,block);h.assertTrue(!place(h,player,Direction.UP),"non-drillable substrate rejected");
        }
        h.setBlock(SUPPORT,Blocks.STONE);h.setBlock(SUPPORT.above(),Blocks.STONE);
        h.assertTrue(!place(h,player,Direction.UP),"occupied target rejected");
        h.setBlock(SUPPORT.above(),Blocks.AIR);
        player.getMainHandItem().getOrCreateTag().putLong("gt.charge",99);
        h.assertTrue(!place(h,player,Direction.UP),"insufficient power rejected");
        h.assertTrue(player.getInventory().items.get(35).getCount()==3
                && player.getMainHandItem().getTag().getLong("gt.charge")==99,"failure does not consume anything");
        for(var block:java.util.List.of(Blocks.DIRT,Blocks.MYCELIUM,Blocks.CLAY,Blocks.SNOW_BLOCK,
                Blocks.SANDSTONE,Blocks.RED_TERRACOTTA,Blocks.INFESTED_STONE,Blocks.DIAMOND_ORE)) {
            h.setBlock(SUPPORT,block);
            h.assertTrue(DynamiteSubstrates.canDrill(h.getLevel(),h.absolutePos(SUPPORT)),"GT6 explicit substrate preserved: "+block);
        }
        h.succeed();
    }
    public static final class Protection {
        boolean seen;
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void cancel(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
            if(event.getPlacedBlock().getBlock() instanceof DynamiteBlock) {seen=true;event.setCanceled(true);}
        }
    }
    @GameTest(template="test_empty")
    public static void cancelledForgePlacementRollsBackWithoutChargingDrill(GameTestHelper h) {
        h.setBlock(SUPPORT,Blocks.STONE);var player=player(h,false);
        var remote=ItemBehaviors.stack("gregtech:remote_activator");player.getInventory().items.set(1,remote);
        var protection=new Protection();net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(protection);
        boolean placed;
        try {placed=place(h,player,Direction.UP);}
        finally {net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(protection);}
        h.assertTrue(protection.seen && !placed && h.getBlockState(SUPPORT.above()).isAir(),"Forge cancellation restores world");
        h.assertTrue(player.getInventory().items.get(35).getCount()==3
                && player.getMainHandItem().getTag().getLong("gt.charge")==1000
                && BehaviorRemote.getCoords(remote.getTag(),h.getLevel()).isEmpty(),"cancellation preserves inventory, energy and remote");
        h.succeed();
    }
}
