package gregtech.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?}

/** Real provider/position validity contracts, not a network packet or auto-close test. */
@GameTestHolder("gregtech_issue_audit")
@PrefixGameTestTemplate(false)
public final class GarbageMenuValidityTests {
    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void garbageBinMenuRequiresLiveNearbyOwner(GameTestHelper h) {
        verify(h, "ender_garbage_bin");
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void garbageRecyclerMenuRequiresLiveNearbyOwner(GameTestHelper h) {
        verify(h, "ender_garbage_dump");
        h.succeed();
    }

    private static void verify(GameTestHelper h, String path) {
        var local = new BlockPos(1, 1, 1);
        var pos = h.absolutePos(local);
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6ce", path));
        h.setBlock(local, block);
        var owner = h.getLevel().getBlockEntity(pos);
        h.assertTrue(owner instanceof MenuProvider, "registered garbage block provides a menu");
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "GarbageValidity"));
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        var menu = ((MenuProvider) owner).createMenu(1, player.getInventory(), player);
        h.assertTrue(menu != null && menu.stillValid(player), "live owner accepts nearby player");
        player.setPos(pos.getX() + 8.5, pos.getY() + .5, pos.getZ() + .5);
        h.assertTrue(menu.stillValid(player), "exact eight-block boundary remains valid");
        player.setPos(pos.getX() + 8.51, pos.getY() + .5, pos.getZ() + .5);
        h.assertTrue(!menu.stillValid(player), "beyond eight blocks is invalid");
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        h.assertTrue(menu.stillValid(player), "returning near a live owner is valid");

        var otherLevel = h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(otherLevel != null && otherLevel != h.getLevel(), "standard GameTest server exposes a second dimension");
        var otherPlayer = FakePlayerFactory.get(otherLevel, new GameProfile(UUID.randomUUID(), "GarbageOther"));
        otherPlayer.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        h.assertTrue(!menu.stillValid(otherPlayer), "same coordinates in another dimension are invalid");

        owner.setRemoved();
        h.assertTrue(!menu.stillValid(player), "removed owner is invalid even before replacement");
        owner.clearRemoved();
        h.assertTrue(menu.stillValid(player), "same live owner is valid after removal flag restored");
        h.setBlock(local, Blocks.AIR);
        h.assertTrue(!menu.stillValid(player), "destroyed block invalidates retained menu");
        h.setBlock(local, block);
        var replacement = h.getLevel().getBlockEntity(pos);
        h.assertTrue(replacement != null && replacement != owner, "same-block placement creates a different owner");
        // Isolate the identity guard from the removed guard: a stale, apparently live
        // object must not gain access to the replacement at the same coordinates.
        owner.clearRemoved();
        h.assertTrue(!menu.stillValid(player), "stale owner cannot bind to a replacement block entity");
        owner.setRemoved();
        var replacementMenu = ((MenuProvider) replacement).createMenu(2, player.getInventory(), player);
        h.assertTrue(replacementMenu != null && replacementMenu.stillValid(player), "replacement's own menu is valid");
    }
}
