package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.PrefixRegistry;
import com.gregtech.gregtech.blockentity.tool.MaterialAnvilBlockEntity;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTToolBlocks;
import com.gregtech.gregtech.registry.GTToolItems;
import com.gregtech.gregtech.registry.GTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Uses a real anvil block the way a player does: place an ore form, then hit it with a hammer.
 *
 * <p>The recipe table ({@code AnvilShreddingRecipes}) and the interaction path
 * ({@code MaterialAnvilBlockEntity#interact}) are two different code paths, so a test on the table
 * alone does not prove that smashing ore actually works in game.</p>
 */
@GameTestHolder("gregtech")
@PrefixGameTestTemplate(false)
public final class AnvilSmashingTests {

    @GameTest(template = "test_empty", timeoutTicks = 400)
    public static void oreFormsSmashWithAHammer(GameTestHelper helper) {
        GTMaterial material = firstOreMaterial();
        helper.assertTrue(material != null, "the port has a mortar-grindable ore material");

        int checked = 0;
        for (String prefixName : new String[]{"crushed", "oreRaw", "rockGt"}) {
            MaterialPrefix prefix = PrefixRegistry.byName(prefixName);
            if (prefix == null) continue;
            ItemStack input = GTItems.getStack(prefix, material, 1);
            if (input.isEmpty()) continue;
            ItemStack output = smash(helper, input.copy());
            helper.assertTrue(!output.isEmpty(),
                    "hammering " + prefixName + " of " + material.getName() + " produces an output");
            helper.assertTrue(!ItemStack.isSameItemSameTags(input, output),
                    "hammer actually processes the input instead of picking it back up");
            checked++;
        }
        helper.assertTrue(checked >= 2, "ore forms exercised: " + checked);
        helper.succeed();
    }

    /** Places the stack on the anvil and hits the top face with a hard hammer. */
    private static ItemStack smash(GameTestHelper helper, ItemStack input) {
        BlockPos pos = new BlockPos(2, 2, 2);
        // §112: every call used to place the anvil on the same position with the same state, and
        // `LevelChunk.setBlockState` returns early for an identical state (§107), so the *previous*
        // call's anvil block entity - workpiece and all - survived into the next one. That is why the
        // second prefix of the loop could fail while the first succeeded. Remove the block first, so
        // each call really starts from a fresh, empty anvil.
        helper.getLevel().removeBlock(helper.absolutePos(pos), false);
        helper.setBlock(pos, GTToolBlocks.ANVILS.get(0).get().defaultBlockState());
        var absolute = helper.absolutePos(pos);
        var anvil = (MaterialAnvilBlockEntity) helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(anvil != null && anvil.workpiece(0).isEmpty() && anvil.workpiece(1).isEmpty(),
                "the anvil is fresh and empty");
        Player player = new Player(helper.getLevel(), absolute, 0,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "anvil-test")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        player.getInventory().clearContent();

        // 1. place the workpiece (top face, left half)
        player.setItemInHand(InteractionHand.MAIN_HAND, input.copy());
        anvil.interact(player, InteractionHand.MAIN_HAND, hit(absolute, Direction.UP, 0.25, 0.9, 0.25));
        if (anvil.workpiece(0).isEmpty() && anvil.workpiece(1).isEmpty()) return ItemStack.EMPTY;

        // 2. hit it with the hammer
        var hammer = GTToolItems.get(com.gregtech.gregtech.api.tool.GTToolType.HARD_HAMMER);
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer == null
                ? ItemStack.EMPTY : com.gregtech.gregtech.item.GTToolItem.create(
                        com.gregtech.gregtech.api.tool.GTToolType.HARD_HAMMER,
                        com.gregtech.gregtech.content.material.Materials.Steel,
                        com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Wood")));
        anvil.interact(player, InteractionHand.MAIN_HAND, hit(absolute, Direction.UP, 0.25, 0.9, 0.25));
        return player.getInventory().items.stream()
                .filter(stack -> !stack.isEmpty() && !com.gregtech.gregtech.api.tool.GTToolHelper.matchesTool(
                        stack, com.gregtech.gregtech.api.tool.GTToolType.HARD_HAMMER))
                .findFirst().orElse(ItemStack.EMPTY);
    }

    private static BlockHitResult hit(net.minecraft.core.BlockPos pos, Direction side,
                                      double x, double y, double z) {
        return new BlockHitResult(new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z), side, pos, false);
    }

    private static GTMaterial firstOreMaterial() {
        for (GTMaterial material : GTMaterialRegistry.allMaterials()) {
            if (!material.isValid()) continue;
            if (!com.gregtech.gregtech.data.generated.MaterialWorkability.isMortarGrindable(material)) continue;
            if (material.getTargetCrushingMaterial() != material) continue;
            if (GTItems.getStack(MaterialPrefix.crushed, material, 1).isEmpty()) continue;
            if (GTItems.getStack(MaterialPrefix.dust, material, 1).isEmpty()) continue;
            boolean supported = true;
            for (var prefix : new MaterialPrefix[]{MaterialPrefix.crushed, MaterialPrefix.oreRaw, MaterialPrefix.rockGt}) {
                var stack = GTItems.getStack(prefix, material, 1);
                if (stack.isEmpty() || !com.gregtech.gregtech.data.MachineRecipeMaps.Anvil.containsInput(stack)) supported = false;
            }
            if (!supported) continue;
            return material;
        }
        return null;
    }
}
