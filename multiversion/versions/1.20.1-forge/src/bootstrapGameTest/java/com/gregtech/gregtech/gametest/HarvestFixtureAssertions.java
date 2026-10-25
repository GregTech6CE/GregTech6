package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.content.cover.PanelCoverHost;
import com.gregtech.gregtech.content.cover.PanelCoverRuntime;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/** Real drops in a dedicated 9-cube template: configured covers travel on the host item. */
final class HarvestFixtureAssertions {
    static void destroyWithCover(GameTestHelper h, BlockEntity owner, Direction side, int value) {
        var level=h.getLevel(); var pos=owner.getBlockPos(); var state=owner.getBlockState();
        var cover=((PanelCoverHost)owner).getCover(side).copy();
        h.assertTrue(!cover.isEmpty(),"cover installed before harvest");
        var positions=new java.util.HashSet<net.minecraft.core.BlockPos>();
        h.forEveryBlockInStructure(p->positions.add(h.absolutePos(p)));
        h.assertTrue(positions.contains(pos),"harvest stays inside dedicated template");
        h.assertTrue(level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(pos))
                &&level.isPositionEntityTicking(pos),"harvest entity chunk loaded and ticking");
        var area=new AABB(pos).inflate(2);
        h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,area).isEmpty(),"harvest area starts without item entities");
        h.assertTrue(level.destroyBlock(pos,true),"native destruction succeeds");
        h.runAfterDelay(1,()->{
            var drops=level.getEntitiesOfClass(ItemEntity.class,area);
            h.assertTrue(drops.stream().filter(e->e.getItem().is(cover.getItem())).mapToInt(e->e.getItem().getCount()).sum()==0,
                    "retained cover is not also dropped separately");
            var hosts=drops.stream().map(ItemEntity::getItem).filter(s->s.is(state.getBlock().asItem())).toList();
            h.assertTrue(hosts.stream().mapToInt(ItemStack::getCount).sum()==1,"exactly one host item dropped");
            var tag=hosts.get(0).getTagElement("BlockEntityTag");
            h.assertTrue(tag!=null,"host item contains cover data");
            var restored=((EntityBlock)state.getBlock()).newBlockEntity(pos,state);
            restored.load(tag);
            var attached=((PanelCoverHost)restored).getCover(side);
            h.assertTrue(attached.getCount()==1&&attached.is(cover.getItem()),"same cover restored exactly once on host");
            h.assertTrue(PanelCoverRuntime.value(attached)==value,"configured cover value restored");
            drops.forEach(ItemEntity::discard);
            h.succeed();
        });
    }
}
