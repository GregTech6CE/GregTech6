package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.blockentity.inventory.MetalChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("gregtech_reports") @PrefixGameTestTemplate(false)
public final class ReportChestTests {
    @GameTest(template="test_empty", timeoutTicks=60)
    public static void placedLootChestDefersLootAndExperienceUntilOpening(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,2,2));
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","loot_chest_dungeon"));
        level.setBlockAndUpdate(pos,block.defaultBlockState());
        var chest=(MetalChestBlockEntity)level.getBlockEntity(pos);
        chest.setDungeonLoot(ResourceLocation.fromNamespaceAndPath("gregtech_reports","paper"),12345);
        var area=new AABB(pos).inflate(2);
        level.getEntitiesOfClass(ExperienceOrb.class,area).forEach(ExperienceOrb::discard);
        h.runAtTickTime(20,()->{
            h.assertTrue(!chest.lootGenerated()&&chest.inventory().getStackInSlot(0).isEmpty(),"placement and twenty real world ticks do not roll loot");
            h.assertTrue(level.getEntitiesOfClass(ExperienceOrb.class,area).isEmpty(),"placing a chest produces no experience orbs");
            var player=h.makeMockSurvivalPlayer();
            var menu=chest.createMenu(1,player.getInventory(),player);
            h.assertTrue(menu!=null&&chest.lootGenerated(),"native GUI opening rolls the pending loot");
            h.assertTrue(level.getEntitiesOfClass(ExperienceOrb.class,area).size()==5,"opening keeps original five loot experience orbs");
            chest.createMenu(2,player.getInventory(),player);
            h.assertTrue(level.getEntitiesOfClass(ExperienceOrb.class,area).size()==5,"second opening never duplicates experience");
            h.succeed();
        });
    }
}
