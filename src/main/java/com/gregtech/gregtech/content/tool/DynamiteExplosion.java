package com.gregtech.gregtech.content.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import java.util.ArrayList;

/** GT6 MultiTileEntityDynamite.DynamiteExplosion: fixed volume, full drops, fortune and no item damage. */
public final class DynamiteExplosion extends Explosion {
    private final ServerLevel world;
    private final BlockPos center;
    private final float resistance;
    private final int fortune;
    private DynamiteExplosion(ServerLevel world, BlockPos center, float resistance, int fortune) {
        super(world,null,center.getX()+.5,center.getY()+.5,center.getZ()+.5,1,false,BlockInteraction.DESTROY);
        this.world=world; this.center=center; this.resistance=resistance; this.fortune=fortune;
    }
    public static void detonate(ServerLevel world, BlockPos center, float resistance, int fortune) {
        var blast=new DynamiteExplosion(world,center,resistance,fortune);
        if(ForgeEventFactory.onExplosionStart(world,blast)) return;
        blast.explode();
        blast.finalizeExplosion(true);
    }
    @Override public void explode() {
        for(var candidate:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1))) {
            var state=world.getBlockState(candidate);
            if(state.isAir() || state.is(Blocks.SPAWNER) || state.getDestroySpeed(world,candidate)<0) continue;
            if(state.getExplosionResistance(world,candidate,this)<=resistance) getToBlow().add(candidate.immutable());
        }
        var point=Vec3.atCenterOf(center);
        var entities=new ArrayList<Entity>(world.getEntities(null,new AABB(point.x-2,point.y-2,point.z-2,point.x+2,point.y+2,point.z+2)));
        ForgeEventFactory.onExplosionDetonate(world,this,entities,1);
        for(var entity:entities) if(!(entity instanceof ItemEntity)) entity.hurt(getDamageSource(),2*resistance);
    }
    @Override public void finalizeExplosion(boolean effects) {
        world.playSound(null,center,SoundEvents.GENERIC_EXPLODE,SoundSource.BLOCKS,4,
                (1+(world.random.nextFloat()-world.random.nextFloat())*.2f)*.7f);
        if(effects) world.sendParticles(ParticleTypes.EXPLOSION,center.getX()+.5,center.getY()+.5,center.getZ()+.5,1,0,0,0,0);
        ItemStack tool=new ItemStack(Items.DIAMOND_PICKAXE);
        tool.enchant(Enchantments.BLOCK_FORTUNE,fortune);
        // Snapshot the positions: chain explosions can change the world while callbacks run.
        for(var pos:new ArrayList<>(getToBlow())) {
            var state=world.getBlockState(pos);
            if(state.isAir()) continue;
            var be=world.getBlockEntity(pos);
            var drops=state.canDropFromExplosion(world,pos,this)
                    ? state.getDrops(new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.TOOL,tool).withOptionalParameter(LootContextParams.BLOCK_ENTITY,be))
                    : java.util.List.<ItemStack>of();
            state.onBlockExploded(world,pos,this);
            for(var drop:drops) Block.popResource(world,pos,drop);
        }
    }
}
