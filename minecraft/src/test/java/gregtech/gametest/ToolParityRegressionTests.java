package gregtech.gametest;

import gregtech.content.block.GTGrassBlock;
import gregtech.content.block.OreBlock;
import gregtech.content.block.OreHostStone;
import gregtech.content.block.misc.DiggableBlock;
import gregtech.content.block.wood.WoodLogBlock;
import gregtech.content.block.wood.WoodSpecies;
import gregtech.content.data.generated.GT6Materials;
import gregtech.content.item.GTToolItem;
import gregtech.content.item.PocketToolItem;
import gregtech.content.registry.GTTreeHoles;
import gregtech.content.registry.GTToolItems;
import gregtech.content.registry.GTWoods;
import gregtech.content.tool.DynamiteSubstrates;
import gregtech.engine.tool.GTToolHelper;
import gregtech.engine.tool.GTToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * Tool durability and dynamite substrate rules that both nodes must share: GT6 drills a sap hole
 * for ten durability on either hand, a pocket tool keeps its damage, name and material when its
 * mode changes, and dynamite sits on GT6's drillable soils and ores but never on bedrock.
 */
@GameTestHolder("gt6ce")
@PrefixGameTestTemplate(false)
public final class ToolParityRegressionTests {
    private static final String BATCH = "tool_parity";
    private static final int SAP_HOLE_DURABILITY = 10;
    private static final int POCKET_DAMAGE = 37;
    private static final String POCKET_NAME = "named pocket tool";

    private ToolParityRegressionTests() {}

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void handDrillSapHoleCostsTenDurabilityInMainHand(GameTestHelper helper) {
        assertSapHoleCostsTenDurability(helper, InteractionHand.MAIN_HAND);
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void handDrillSapHoleCostsTenDurabilityInOffHand(GameTestHelper helper) {
        assertSapHoleCostsTenDurability(helper, InteractionHand.OFF_HAND);
    }

    /** GT6 {@code BlockTreeLogA} drills the sap hole for a 1000 return, i.e. ten durability. */
    private static void assertSapHoleCostsTenDurability(GameTestHelper helper, InteractionHand hand) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Block hole = GTTreeHoles.hole(WoodSpecies.MAPLE);
        helper.assertTrue(hole != null, "maple logs have a sap hole");
        WoodLogBlock log = GTWoods.log(WoodSpecies.MAPLE);
        BlockState state = log.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        ItemStack drill = GTToolItem.create(
                GTToolType.HAND_DRILL, GT6Materials.Compounds.Steel, GT6Materials.Woods.Spruce);
        helper.assertTrue(GTToolHelper.matchesTool(drill, GTToolType.HAND_DRILL),
                "the hand drill fixture has tool stats");
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(hand, drill);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);

        InteractionResult result = log.use(state, level, pos, player, hand, hit);

        helper.assertTrue(result.consumesAction(), "the hand drill drills the log");
        helper.assertTrue(level.getBlockState(pos).is(hole), "the log becomes its sap hole");
        helper.assertTrue(drill.getDamageValue() == SAP_HOLE_DURABILITY,
                "drilling costs ten GT tool durability, not one vanilla point");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void pocketToolModeSwitchKeepsDamageNameAndMaterial(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        var steel = GT6Materials.Compounds.Steel;
        ItemStack pocket = GTToolItem.create(
                GTToolType.POCKET_MULTITOOL, steel, GT6Materials.Woods.Spruce);
        helper.assertTrue(pocket.getItem() instanceof PocketToolItem, "the pocket tools are registered");
        pocket.setDamageValue(POCKET_DAMAGE);
        setCustomName(pocket, POCKET_NAME);
        enchantMending(level, pocket);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, pocket);
        player.setShiftKeyDown(true);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        UseOnContext context = new UseOnContext(level, player, InteractionHand.MAIN_HAND,
                player.getItemInHand(InteractionHand.MAIN_HAND), hit);

        InteractionResult result = ((PocketToolItem) pocket.getItem()).onItemUseFirst(pocket, context);

        helper.assertTrue(result.consumesAction(), "shift-clicking a block switches the pocket mode");
        ItemStack next = player.getItemInHand(InteractionHand.MAIN_HAND);
        helper.assertTrue(next.getItem() == GTToolItems.get(GTToolType.POCKET_KNIFE),
                "the pocket multitool switches to the pocket knife");
        helper.assertTrue(next.getDamageValue() == POCKET_DAMAGE, "the switch keeps the tool's damage");
        helper.assertTrue(next.getHoverName().getString().equals(POCKET_NAME),
                "the switch keeps the custom name");
        helper.assertTrue(GTToolHelper.getHead(next) == steel, "the switch keeps the head material");
        helper.assertTrue(mendingLevel(level, next) > 0, "the switch keeps the player's enchantment");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = BATCH, timeoutTicks = 20)
    public static void dynamiteSubstratesAcceptSoilAndOresButNotBedrock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        assertDrillable(helper, level, pos, Blocks.GRASS_BLOCK.defaultBlockState(), "grass block");
        assertDrillable(helper, level, pos, Blocks.DIRT.defaultBlockState(), "dirt");
        assertDrillable(helper, level, pos, Blocks.GRAVEL.defaultBlockState(), "gravel");
        assertDrillable(helper, level, pos, Blocks.CLAY.defaultBlockState(), "clay");
        assertDrillable(helper, level, pos, Blocks.IRON_ORE.defaultBlockState(), "vanilla iron ore");
        Block gtGrass = firstBlock(GTGrassBlock.class);
        helper.assertTrue(gtGrass != null, "GT grass blocks are registered");
        assertDrillable(helper, level, pos, gtGrass.defaultBlockState(), "GT grass block");
        Block diggable = firstBlock(DiggableBlock.class);
        helper.assertTrue(diggable != null, "diggable surface blocks are registered");
        assertDrillable(helper, level, pos, diggable.defaultBlockState(), "diggable surface block");

        level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
        helper.assertFalse(DynamiteSubstrates.canDrill(level, pos), "bedrock never carries dynamite");
        Block ore = firstBlock(OreBlock.class);
        helper.assertTrue(ore != null, "GT ores are registered");
        level.setBlockAndUpdate(pos, ((OreBlock) ore).stateFor(OreHostStone.BEDROCK));
        helper.assertFalse(DynamiteSubstrates.canDrill(level, pos),
                "bedrock-host ores never carry dynamite");
        helper.succeed();
    }

    private static void assertDrillable(GameTestHelper helper, ServerLevel level, BlockPos pos,
                                        BlockState state, String what) {
        level.setBlockAndUpdate(pos, state);
        helper.assertTrue(DynamiteSubstrates.canDrill(level, pos), what + " carries dynamite");
    }

    private static Block firstBlock(Class<? extends Block> type) {
        for (Block block : BuiltInRegistries.BLOCK) if (type.isInstance(block)) return block;
        return null;
    }

    /** A real server player in survival mode, so the creative and use gates behave as in play. */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "tool-parity-test");
        var connection = new net.minecraft.network.Connection(
                net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
//? if >=1.21.1 {
        /*ServerPlayer player = new ServerPlayer(level.getServer(), level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                level.getServer(), connection, player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
*///?} else {
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                level.getServer(), connection, player) {
//?}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,
                                       net.minecraft.network.PacketSendListener listener) {}
        };
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void setCustomName(ItemStack stack, String name) {
//? if >=1.21.1 {
        /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name));
*///?} else {
        stack.setHoverName(Component.literal(name));
//?}
    }

    private static void enchantMending(ServerLevel level, ItemStack stack) {
//? if >=1.21.1 {
        /*var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        stack.enchant(registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING), 1);
*///?} else {
        stack.enchant(net.minecraft.world.item.enchantment.Enchantments.MENDING, 1);
//?}
    }

    private static int mendingLevel(ServerLevel level, ItemStack stack) {
//? if >=1.21.1 {
        /*var registry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING), stack);
*///?} else {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.MENDING, stack);
//?}
    }
}
