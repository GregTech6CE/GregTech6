package com.gregtech.gregtech.gametest;

import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.GameType;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.item.GTToolItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@GameTestHolder("gregtech_issues")
@PrefixGameTestTemplate(false)
public final class IssueSpringTests {
    @GameTest(template="test_empty",timeoutTicks=30)
    public static void storedFluidIsInClientUpdateAndModelData(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,2,2));var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gregtech","fluid_spring"));
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
        var spring=(com.gregtech.gregtech.blockentity.FluidSpringBlockEntity)h.getLevel().getBlockEntity(pos);
        spring.setSpring("gregtech:liquid_light_oil",6000);var tag=spring.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(tag.getString("spring").equals("gregtech:liquid_light_oil")&&tag.getInt("amount")==6000,"stored fluid reaches client packet");
        h.assertTrue(spring.getUpdatePacket()!=null&&spring.getModelData().get(com.gregtech.gregtech.blockentity.FluidSpringBlockEntity.MODEL_FLUID)==spring.fluid(),"actual model data uses stored fluid");h.succeed();
    }
}
