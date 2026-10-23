package com.gregtech.gregtech.gametest;

import com.gregtech.gregtech.block.misc.SpikeBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import com.gregtech.gregtech.registry.GTSpikes;
import com.gregtech.gregtech.bootstrap.registration.OriginCreativeContents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class SpikeDeliveryAuditTests {
    /** Existing native pick-block producer; no new item class, fixture recipe or arbitrary NBT. */
    private static ItemStack variant(GameTestHelper h, SpikeBlock block, SpikeBlock.Mode mode, boolean secondary) {
        var state = block.defaultBlockState().setValue(SpikeBlock.MODE, mode).setValue(SpikeBlock.SECONDARY, secondary);
        var pos = h.absolutePos(new net.minecraft.core.BlockPos(4, 20, 4));
//? if >=1.21.1 {
        /*return block.getCloneItemStack(state, new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false), h.getLevel(), pos, null);
*///?} else {
        return block.getCloneItemStack(h.getLevel(), pos, state);
//?}
    }

    @GameTest(template = "test_empty")
    public static void thirtySourceVariantItems(GameTestHelper h) {
        int count = 0;
        for (var registered : GTSpikes.all()) {
            var block = registered.get();
            h.assertTrue(block.asItem() instanceof BlockItem, "existing registered block item");
            for (boolean secondary : new boolean[]{false, true}) {
                for (var mode : SpikeBlock.Mode.values()) {
                    var stack = variant(h, block, mode, secondary);
                    h.assertTrue(SpikeBlock.itemMode(stack) == mode, "native mode roundtrip");
                    h.assertTrue(SpikeBlock.itemSecondary(stack) == secondary, "native material roundtrip");
                    int meta = (secondary ? 8 : 0) + (mode == SpikeBlock.Mode.WALL ? 0 : mode == SpikeBlock.Mode.OMNI ? 6 : 7);
                    var name = stack.getHoverName().getContents();
                    h.assertTrue(name instanceof TranslatableContents t && t.getKey().equals("gt.block.spikes."
                            + block.family().name().toLowerCase(java.util.Locale.ROOT) + "." + meta), "source subtype name");
                    count++;
                }
            }
        }
        h.assertTrue(count == 30, "five families times two materials times three forms");
        long shown = OriginCreativeContents.pages().values().stream().flatMap(java.util.Collection::stream)
                .filter(s -> s.getItem() instanceof BlockItem b && b.getBlock() instanceof SpikeBlock).count();
        h.assertTrue(shown == 30, "creative exposes all source variants, not five default items");
        h.succeed();
    }
}
