package com.gregtech.gregtech.item.behavior;

import com.gregtech.gregtech.api.tool.GTToolHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * GT6 {@code Behavior_Plunger_Fluid} ({@code Behavior_Plunger_Fluid.java:41-73}) — the plunger's
 * "empty the tank" click.
 *
 * <h2>What the original does</h2>
 *
 * <p>One click, on the server only ({@code :50}): resolve the block entity
 * ({@code :51}), and for each of the six sides in GT6's own order, drain a <em>simulated</em>
 * {@code 1000} mB ({@code :53}). The first side that reports any fluid at all is the one that
 * matters: the tool pays {@code mCosts} durability ({@code :54}) and only then is the drain really
 * executed, followed by GT6's trampoline sound ({@code :55-56}). When the tool cannot pay — broken,
 * or {@code doDamage} refused — the tank keeps its fluid, because the simulated drain never changed
 * anything. When no side has fluid the click does nothing and reports {@code F}.</p>
 *
 * <p>The costs are the plunger tool's own {@code getToolDamagePerDropConversion()}
 * ({@code GT_Tool_Plunger:87} passes it to the behaviour); the plunger does not override it, so
 * {@code ToolStats:64} applies and the number is {@link #COSTS}.</p>
 *
 * <h2>Port notes</h2>
 *
 * <ul>
 *   <li>{@code WD.te(aWorld, aX, aY, aZ, T)} is GT6's delegating tile-entity lookup, which is how a
 *       covering multi-block hands the click on. The port resolves the plain block entity, the same
 *       simplification every other behaviour in this package makes.</li>
 *   <li>1.7.10's {@code IFluidHandler.drain(side, maxDrain, doDrain)} returns {@code null} for "no
 *       fluid"; Forge's 1.20.1 {@code drain(int, FluidAction)} returns an empty
 *       {@link FluidStack}. The original only ever tests for {@code null}, so a partial drain also
 *       counts as "there is fluid here" — the port keeps that: any non-empty simulate result is
 *       enough, the real drain then takes up to {@code 1000} mB.</li>
 *   <li>GT6's creative bypass lives in {@code MultiItemTool.doDamage} ({@code MultiItemTool:434}:
 *       {@code hasInfiniteItems(aPlayer)} returns {@code T} without damaging), and a tool that cannot
 *       take the damage is rejected before anything happens ({@code :435}). Both are kept:
 *       {@link GTToolHelper#isUsable} is the port's {@code isItemStackUsable}.</li>
 * </ul>
 *
 * <h2>Sibling behaviours</h2>
 *
 * <p>{@code Behavior_Plunger_Item} (item pipes) is entirely commented out in the original
 * ({@code Behavior_Plunger_Item.java:42-64}) and {@code Behavior_Plunger_Essentia}
 * ({@code :41-71}) needs Thaumcraft's {@code IEssentiaTransport}; neither is implemented here.</p>
 */
public final class BehaviorPlungerFluid {

    /** GT6 {@code Behavior_Plunger_Fluid:53}: {@code drain(tDirection, 1000, F)}. */
    public static final int DRAIN_AMOUNT = 1000;

    /** GT6 {@code ToolStats:64} via {@code GT_Tool_Plunger:87}: {@code getToolDamagePerDropConversion()}. */
    public static final int COSTS = 100;

    private BehaviorPlungerFluid() {}

    /**
     * GT6 {@code Behavior_Plunger_Fluid:49-62}: clear up to {@link #DRAIN_AMOUNT} mB from the first
     * tank face of the block entity that has fluid, paying {@code costs} durability for it.
     *
     * <p>The order of the original is preserved: find a face with fluid (simulate), then check that
     * the tool can pay, then drain for real. That ordering is why a broken plunger leaves the tank
     * untouched.</p>
     *
     * @param level  the level the clicked block stands in
     * @param pos    the clicked block; its block entity has to be an {@link IFluidHandler}
     * @param player the plunger's holder, or {@code null} for GT6's auto-tool case
     * @param plunger the plunger stack, damaged by {@code costs} unless the holder is in creative
     * @param costs  GT6's {@code mCosts} — {@link #COSTS} for the port's plunger
     * @return whether fluid was drained, i.e. whether the tool was damaged
     */
    public static boolean plungeFluid(Level level, BlockPos pos, @Nullable Player player, ItemStack plunger,
                                      int costs) {
        if (level.isClientSide) return false;                                  // :50
        if (level.getBlockEntity(pos)==null) return false; // :51-52

        for (Direction side : Direction.values()) {                            // :53
            IFluidHandler handler=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,side);
            if(handler==null)continue;
            FluidStack simulated = handler.drain(DRAIN_AMOUNT, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) continue;
            if (!pay(plunger, costs, player)) return false;                    // :54
            FluidStack drained = handler.drain(DRAIN_AMOUNT, IFluidHandler.FluidAction.EXECUTE); // :55
            return !drained.isEmpty();
        }
        return false;
    }

    /**
     * GT6's {@code MultiItemTool.doDamage(aStack, mCosts, aPlayer, F)} ({@code MultiItemTool:433-435}):
     * creative pays nothing, a tool that is not usable is refused, everything else takes the damage.
     *
     * @return whether the cost could be paid
     */
    public static boolean pay(ItemStack plunger, int costs, @Nullable Player player) {
        if (ManualToolBehaviorAccess.creative(player)) return true;                       // :434
        if (!usable(plunger)) return false;                                    // :435
        GTToolHelper.damageForUse(plunger, costs, player);
        return true;
    }

    /**
     * GT6 {@code MultiItemTool.isItemStackUsable} in the port's tool model.
     *
     * <p>{@link GTToolHelper#isUsable} demands {@code GT.ToolStats}, which a tool straight out of the
     * creative tab does not have — and such a tool reports a placeholder durability of {@code 1}
     * ({@link GTToolHelper#getMaxDurability}). This uses the same test
     * {@link GTToolHelper#matchesTool} applies to headless tools, so a real assembled plunger and a
     * bare one are treated alike and neither is rejected for the wrong reason.</p>
     */
    public static boolean usable(ItemStack plunger) {
        if (plunger.isEmpty()) return false;
        return !plunger.isDamageableItem() || plunger.getDamageValue() < plunger.getMaxDamage();
    }
}
