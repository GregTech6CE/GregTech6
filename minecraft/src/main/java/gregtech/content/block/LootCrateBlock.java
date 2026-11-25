package gregtech.content.block;

import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import gregtech.platform.Mc;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * GT6's Loot Crate ({@code MultiTileEntityLootCrate}): a crate lying around that a player pries open
 * with a <em>crowbar</em>, which drops one random <em>vanilla</em> loot roll plus the crate itself
 * (GT6 {@code ST.generateOneVanillaLoot()} + {@code IL.Crate.get(1)}, {@code onToolClick}).
 *
 * <p>GT6 rolls {@code ChestGenHooks.getOneItem(UT.Code.select("dungeonChest", LOOT_TABLES_VANILLA))}
 * — a random vanilla chest table with the dungeon chest as the fallback. The port keeps that: the ten
 * vanilla tables GT6's loot loader also touches, with {@code chests/simple_dungeon} first.
 */
public class LootCrateBlock extends GTBlock {
    /** GT6 {@code onToolClick}: a crowbar pries the crate open. */
    @Override
    protected InteractionResult useBlock(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!GTToolHelper.matchesTool(held, GTToolType.CROWBAR)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.PASS;
        List<ItemStack> loot = rollVanillaLoot(server, pos, level.random);
        if (!level.removeBlock(pos, false)) return InteractionResult.PASS;
        for (ItemStack stack : loot) popResource(level, pos, stack);
        // GT6 also hands the crate itself back (IL.Crate, the port's icon set "crate" block).
        ItemStack crate = crateStack();
        if (!crate.isEmpty()) popResource(level, pos, crate);
        GTToolHelper.damageForToolClickReturn(held, 500, player);
        return InteractionResult.CONSUME;
    }

    /** GT6's {@code LOOT_TABLES_VANILLA} as 1.20.1 loot tables, dungeon chest first (its fallback). */
    public static final List<ResourceLocation> VANILLA_TABLES = List.of(
            ResourceLocation.withDefaultNamespace("chests/simple_dungeon"),
            ResourceLocation.withDefaultNamespace("chests/abandoned_mineshaft"),
            ResourceLocation.withDefaultNamespace("chests/desert_pyramid"),
            ResourceLocation.withDefaultNamespace("chests/jungle_temple"),
            ResourceLocation.withDefaultNamespace("chests/jungle_temple_dispenser"),
            ResourceLocation.withDefaultNamespace("chests/stronghold_library"),
            ResourceLocation.withDefaultNamespace("chests/stronghold_corridor"),
            ResourceLocation.withDefaultNamespace("chests/stronghold_crossing"),
            ResourceLocation.withDefaultNamespace("chests/village/village_weaponsmith"),
            ResourceLocation.withDefaultNamespace("chests/spawn_bonus_chest"));

    public LootCrateBlock(Properties properties) {
        super(properties);
    }

    /** GT6 {@code UT.Code.select("dungeonChest", LOOT_TABLES_VANILLA)} — a random table of the list. */
    public static ResourceLocation randomTable(RandomSource random) {
        return VANILLA_TABLES.get(random.nextInt(VANILLA_TABLES.size()));
    }

    /** Run a random modern table, then choose one output stack; GT6 chose one weighted entry. */
    public static List<ItemStack> rollVanillaLoot(ServerLevel level, BlockPos pos, RandomSource random) {
        LootTable table = gregtech.engine.stack.LootCompat.getLootTable(level.getServer(), randomTable(random));
        if (table == LootTable.EMPTY) return List.of();
        net.minecraft.world.level.storage.loot.LootParams params =
                new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(
                                net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                                net.minecraft.world.phys.Vec3.atCenterOf(pos))
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        var candidates = table.getRandomItems(params).stream()
                .filter(stack -> !stack.isEmpty())
                .toList();
        // GT6 returns one generated stack, rather than all contents of a chest.
        return candidates.isEmpty()
                ? List.of()
                : List.of(gregtech.engine.material.ItemMaterialForms.canonical(
                        candidates.get(random.nextInt(candidates.size()))));
    }

    /** GT6's {@code IL.Crate} — the port's {@code gt6ce:crate} block. */
    public static ItemStack crateStack() {
        return crateStack("crate");
    }

    /** {@code IL.Crate_Fireproof} resolves through the same meta-item family in GT6. */
    public static ItemStack crateStack(String id) {
        var block = Mc.block(ResourceLocation.fromNamespaceAndPath("gt6ce", id));
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    @Override
    protected void appendTooltip(
            ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.gt6ce.loot_crate.tooltip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
