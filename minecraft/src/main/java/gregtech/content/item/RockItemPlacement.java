package gregtech.content.item;

import gregtech.content.block.RockBlock;
import gregtech.content.blockentity.RockBlockEntity;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.registry.GTBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Replaces one collected pebble, preserving its material and never recreating an ore bonus. */
public final class RockItemPlacement {
    private RockItemPlacement() {}

    public static InteractionResult place(UseOnContext use) {
        ItemStack stack = use.getItemInHand();
        var form = gregtech.engine.material.MaterialEquivalence.form(stack);
        if (use.getPlayer() == null
                || !use.getPlayer().isShiftKeyDown()
                || form == null
                || form.prefix() != MaterialPrefix.rockGt && form.prefix() != MaterialPrefix.oreRaw)
            return InteractionResult.PASS;
        return placeLooseRock(
                use,
                form.material().resolve().getName(),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    /** GT6 {@code GT_Proxy.java:297}: flint put down by hand becomes a lying-item rock. */
    public static InteractionResult placeFlint(UseOnContext use) {
        if (use.getPlayer() == null
                || !use.getPlayer().isShiftKeyDown()
                || !use.getItemInHand().is(net.minecraft.world.item.Items.FLINT)) return InteractionResult.PASS;
        return placeLooseRock(use, null, "minecraft:flint");
    }

    private static InteractionResult placeLooseRock(UseOnContext use, String material, String itemId) {
        Player player = use.getPlayer();
        ItemStack stack = use.getItemInHand();
        BlockPlaceContext context = new BlockPlaceContext(use);
        var state = GTBlocks.ROCK
                .get()
                .defaultBlockState()
                .setValue(
                        RockBlock.GROUND,
                        RockBlock.Ground.of(context.getLevel()
                                .getBlockState(context.getClickedPos().below())));
        return GroundItemPlacement.place(
                context,
                state,
                (level, pos, placed, placedBy) -> level.isUnobstructed(placed, pos, CollisionContext.of(placedBy)),
                (level, pos, placed, held) -> {
                    if (!(level.getBlockEntity(pos) instanceof RockBlockEntity rock)) return 0;
                    if (material != null && !material.isEmpty()) rock.setMaterial(material);
                    rock.setItemId(itemId);
                    rock.setCount(1);
                    rock.setRawOre(false);
                    player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                    return 1;
                });
    }
}
