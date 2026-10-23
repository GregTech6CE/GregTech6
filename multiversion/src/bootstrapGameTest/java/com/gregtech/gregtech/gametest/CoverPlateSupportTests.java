package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.energy.SignalWireBlock;
import com.gregtech.gregtech.content.cover.CoverWorldInteraction;
import com.gregtech.gregtech.content.cover.PanelCoverHost;
import com.gregtech.gregtech.registry.GTSignalWires;
import com.gregtech.gregtech.registry.GTTechnological;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Ignored native draft: existing physical collision, ordinary outline, and dynamic attachment support. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class CoverPlateSupportTests {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private record Saved(BlockState state, CompoundTag data) {}
    private static SignalWireBlock wire(boolean insulated) {
        return GTSignalWires.all().stream().map(r -> r.get())
                .filter(b -> b.material().getName().equals("RedAlloy") && b.insulated()==insulated).findFirst().orElseThrow();
    }
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final BlockPos pos;
        final ServerPlayer player;
        final LinkedHashMap<BlockPos,Saved> saved = new LinkedHashMap<>();
        Fixture(GameTestHelper h,int index) {this(h,index,true);}
        Fixture(GameTestHelper h,int index,boolean insulated) {
            this.h=h; pos=new BlockPos(21_960_008+index*32,230,21_960_008);
//? if >=1.21.1 {
/*            player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
*///?} else {
            player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
//?}
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"[gt-cover-solid]"));
            player.setGameMode(GameType.SURVIVAL); player.getInventory().clearContent(); player.setShiftKeyDown(false);
            for(var mutable:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,2,2))) {
                var at=mutable.immutable(); var be=h.getLevel().getBlockEntity(at);
//? if >=1.21.1 {
/*                var data=be==null?null:be.saveWithoutMetadata(h.getLevel().registryAccess());
*///?} else {
                var data=be==null?null:be.saveWithoutMetadata();
//?}
                saved.put(at,new Saved(h.getLevel().getBlockState(at),data));
            }
            saved.keySet().forEach(at->set(at,Blocks.AIR.defaultBlockState()));
            set(pos,wire(insulated).defaultBlockState());
        }
        void set(BlockPos at,BlockState state) {
            h.getLevel().removeBlockEntity(at); h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),FLAGS);
            h.getLevel().setBlock(at,state,FLAGS);
        }
        PanelCoverHost host(){return (PanelCoverHost)h.getLevel().getBlockEntity(pos);}
        void click(Direction side,ItemStack stack) {
            player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var normal=Vec3.atLowerCornerOf(side.getNormal()); var point=Vec3.atCenterOf(pos).add(normal.scale(.5));
            var stand=Vec3.atCenterOf(pos).add(normal.scale(2.5));player.setPos(stand.x,stand.y,stand.z);
            player.gameMode.useItemOn(player,h.getLevel(),stack,InteractionHand.MAIN_HAND,new BlockHitResult(point,side,pos,false));
        }
        @Override public void close() {
            player.closeContainer();player.getInventory().clearContent();player.discard();
            saved.keySet().forEach(at->set(at,Blocks.AIR.defaultBlockState()));
            for(var entry:saved.entrySet()) {
                set(entry.getKey(),entry.getValue().state());var be=h.getLevel().getBlockEntity(entry.getKey());
                if(be!=null&&entry.getValue().data()!=null) {
//? if >=1.21.1 {
/*                    be.loadWithComponents(entry.getValue().data(),h.getLevel().registryAccess());
*///?} else {
                    be.load(entry.getValue().data());
//?}
                }
            }
        }
    }
    /** Off-axis ray intersects only the plate, never the central wire or connection arm. */
    private static boolean hitsPlate(VoxelShape shape,BlockPos pos,Direction side) {
        var normal=Vec3.atLowerCornerOf(side.getNormal());
        var point=Vec3.atLowerCornerOf(pos).add(side.getAxis()==Direction.Axis.X?.5:.8,
                side.getAxis()==Direction.Axis.Y?.5:.8,side.getAxis()==Direction.Axis.Z?.5:.8);
        var hit=shape.clip(point.add(normal.scale(1)),point,pos);
        return hit!=null&&hit.getDirection()==side;
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void blankCoverAddsOutlineAndNativeLeverSupport(GameTestHelper h) {
        var observations=new ArrayList<String>();boolean correct=true;
        for(boolean insulated:new boolean[]{false,true})for(var side:Direction.values())try(var f=new Fixture(h,side.ordinal()+(insulated?6:0),insulated)) {
            var state=h.getLevel().getBlockState(f.pos);
            h.assertTrue(!state.isFaceSturdy(h.getLevel(),f.pos,side),"uncovered thin wire has no full support: "+side);
            h.assertTrue(!hitsPlate(state.getCollisionShape(h.getLevel(),f.pos),f.pos,side),"bare wire has no off-axis plate collision");
            var blank=GTTechnological.get("blank_cover");h.assertTrue(blank!=null&&blank!=Items.AIR,"actual registered blank cover");
            f.click(side,new ItemStack(blank,2));
            h.assertTrue(f.host().getCover(side).is(blank)&&f.player.getMainHandItem().getCount()==1,"native blank cover attachment consumes one");
            state=h.getLevel().getBlockState(f.pos);
            // Existing collision should already pass: diagnose outline/support separately.
            boolean collision=hitsPlate(state.getCollisionShape(h.getLevel(),f.pos),f.pos,side);
            boolean contextualCollision=hitsPlate(state.getCollisionShape(h.getLevel(),f.pos,CollisionContext.empty()),f.pos,side);
            h.assertTrue(collision&&contextualCollision,"existing physical plate collision on both native overloads: "+side);
            boolean outline=hitsPlate(state.getShape(h.getLevel(),f.pos,CollisionContext.empty()),f.pos,side);
            boolean supportShape=Block.isFaceFull(state.getBlockSupportShape(h.getLevel(),f.pos),side);
            boolean sturdy=state.isFaceSturdy(h.getLevel(),f.pos,side);
            for(var type:net.minecraft.world.level.block.SupportType.values())sturdy &= state.isFaceSturdy(h.getLevel(),f.pos,side,type);
            boolean underlyingThin=!CoverWorldInteraction.isCoverSurface(f.host().coverOwner(),side);
            f.click(side,new ItemStack(Items.LEVER,2));
            var leverPos=f.pos.relative(side);var lever=h.getLevel().getBlockState(leverPos);
            boolean placed=lever.is(Blocks.LEVER)&&f.player.getMainHandItem().getCount()==1&&lever.canSurvive(h.getLevel(),leverPos);
            var removed=f.host().removeCover(side);
            h.assertTrue(removed.is(blank),"remove actual blank cover for dynamic support check");
            boolean removedSupport=!state.isFaceSturdy(h.getLevel(),f.pos,side);
            boolean removedOutline=!hitsPlate(state.getShape(h.getLevel(),f.pos,CollisionContext.empty()),f.pos,side);
            boolean leverLosesSupport=!placed||!lever.canSurvive(h.getLevel(),leverPos);
            observations.add((insulated?"cable:":"bare:")+side+" collision="+collision+" outline="+outline+" supportShape="+supportShape+" sturdy="+sturdy
                    +" lever="+placed+" underlyingThin="+underlyingThin+" removed="+removedSupport+"/"+removedOutline+"/"+leverLosesSupport);
            correct &= outline&&supportShape&&sturdy&&placed&&underlyingThin&&removedSupport&&removedOutline&&leverLosesSupport;
        }
        h.assertTrue(correct,"blank covers supply selectable solid faces without changing raw host geometry: "+observations);h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=100)
    public static void coverSupportIsFaceLocalAndTorchRemainsNonSolid(GameTestHelper h) {
        try(var f=new Fixture(h,16)) {
            var blank=GTTechnological.get("blank_cover");
            f.click(Direction.EAST,new ItemStack(blank,2));
            h.assertTrue(f.host().getCover(Direction.EAST).is(blank),"real east blank cover attached");
            var state=h.getLevel().getBlockState(f.pos);
            h.assertTrue(!state.isFaceSturdy(h.getLevel(),f.pos,Direction.NORTH),"east cover does not grant north support");
            f.click(Direction.NORTH,new ItemStack(Items.REDSTONE_TORCH,2));
            h.assertTrue(f.host().getCover(Direction.NORTH).is(Items.REDSTONE_TORCH),"actual insulated-wire torch cover");
            h.assertTrue(!state.isFaceSturdy(h.getLevel(),f.pos,Direction.NORTH),"original torch cover is not solid");
            f.click(Direction.NORTH,new ItemStack(Items.LEVER,2));
            h.assertTrue(h.getLevel().getBlockState(f.pos.north()).isAir()&&f.player.getMainHandItem().getCount()==2,"native lever rejects nonsolid torch face");
            f.set(f.pos.west(),Blocks.STONE.defaultBlockState());
            for(var face:Direction.values())for(var type:net.minecraft.world.level.block.SupportType.values())
                h.assertTrue(h.getLevel().getBlockState(f.pos.west()).isFaceSturdy(h.getLevel(),f.pos.west(),face,type),"ordinary full-block support unchanged");
            var pipe=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","pipe_medium_steel"));
            h.assertTrue(pipe instanceof com.gregtech.gregtech.block.machine.FluidPipeBlock,"real pressure-valve host");
            f.set(f.pos,pipe.defaultBlockState());
            h.assertTrue(!h.getLevel().getBlockState(f.pos).isFaceSturdy(h.getLevel(),f.pos,Direction.NORTH),"uncovered medium pipe has no full north support");
            var valve=GTTechnological.get(com.gregtech.gregtech.content.cover.CoverUtilityBehaviors.PRESSURE_VALVE);
            h.assertTrue(valve!=null&&valve!=Items.AIR,"real pressure-valve cover");
            f.click(Direction.NORTH,new ItemStack(valve,2));
            h.assertTrue(f.host().getCover(Direction.NORTH).is(valve)&&f.player.getMainHandItem().getCount()==1,"native valve attachment succeeds");
            var pipeState=h.getLevel().getBlockState(f.pos);
            h.assertTrue(!pipeState.isFaceSturdy(h.getLevel(),f.pos,Direction.NORTH),"original pressure-valve face is not solid");
            f.click(Direction.NORTH,new ItemStack(Items.LEVER,2));
            h.assertTrue(h.getLevel().getBlockState(f.pos.north()).isAir()&&f.player.getMainHandItem().getCount()==2,"native lever rejects pressure-valve face");
            var huge=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","pipe_huge_steel"));
            h.assertTrue(huge instanceof com.gregtech.gregtech.block.machine.FluidPipeBlock,"real full-diameter valve host");
            f.set(f.pos,huge.defaultBlockState());
            h.assertTrue(h.getLevel().getBlockState(f.pos).isFaceSturdy(h.getLevel(),f.pos,Direction.NORTH),"uncovered huge pipe supplies native full support");
            f.click(Direction.NORTH,new ItemStack(valve,2));
            h.assertTrue(f.host().getCover(Direction.NORTH).is(valve)&&f.player.getMainHandItem().getCount()==1,"native valve attaches to full-diameter host");
            h.assertTrue(h.getLevel().getBlockState(f.pos).isFaceSturdy(h.getLevel(),f.pos,Direction.NORTH),"nonsolid valve preserves underlying huge-pipe support");
        }
        h.succeed();
    }
}
