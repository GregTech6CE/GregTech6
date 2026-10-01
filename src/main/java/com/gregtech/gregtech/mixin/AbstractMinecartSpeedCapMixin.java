package com.gregtech.gregtech.mixin;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * §115: lets a GT6 rail actually reach its own speed — the port's replacement for GT6's
 * {@code Minecraft_RemoveCartSpeedCap} ASM transformer.
 *
 * <p>Forge routes a rail's speed through the cart: {@code AbstractMinecart.getMaxSpeedWithRail()} is
 * {@code Math.min(railMaxSpeed, getCurrentCartSpeedCapOnRail())}
 * ({@code AbstractMinecart.java:854-863}), the cap starts at {@code getMaxCartSpeedOnRail()} and
 * <b>Forge's setter clamps every assignment back down to that same ceiling</b>
 * ({@code AbstractMinecart.java:844}: {@code currentSpeedCapOnRail = Math.min(value,
 * getMaxCartSpeedOnRail())}). For a vanilla minecart that ceiling is <b>1.2</b>
 * ({@code IForgeAbstractMinecart.getMaxCartSpeedOnRail()}, whose comment warns that faster carts can
 * outrun chunk loading), so GT6's three fastest rails — Tungstensteel 1.40, Tungstencarbide 1.60 and
 * Adamantium 4.00 ({@code Loader_Rails:41-72}) — were silently capped at 1.2. The port recorded that as
 * a deliberate difference for as long as it had no mixins ({@code TrackBlock}'s class javadoc, §68.3);
 * §105 added the mixin infrastructure, so the difference can be closed.</p>
 *
 * <h2>What this does and does not change</h2>
 *
 * <p>The injection makes the setter a plain assignment, which is exactly what GT6's transformer did
 * (it removed the cap outright). Three properties keep it narrow:</p>
 *
 * <ul>
 *   <li><b>Vanilla rails are untouched.</b> Vanilla never calls
 *       {@code setCurrentCartSpeedCapOnRail}; only a rail that does — the port's {@code TrackBlock},
 *       through {@code applySpeedToCart} — can move the cap, and it hands over the value its own
 *       {@code railSpeed} computed, so a curve still means 0.4 and an unloaded neighbour still means
 *       1.0 ({@code BlockBaseRail:277-289}).</li>
 *   <li><b>A cart that never met a GT6 rail keeps Forge's behaviour</b>: its cap stays at
 *       {@code getMaxCartSpeedOnRail()}, and {@code getMaxCartSpeedOnRail()} itself is not touched —
 *       a rail that asks the cart for its ceiling still gets 1.2.</li>
 *   <li><b>Slower rails still work.</b> A direct assignment also lets the cap come back <em>down</em>
 *       (Adamantium 4.00 then Tungstensteel 1.40), which the vanilla setter would have flattened to
 *       1.2 both times.</li>
 * </ul>
 *
 * <p>The {@code > 0} guard keeps a nonsensical zero/negative assignment out of the field, so a cart
 * cannot end up unable to move at all — the original setter only ever stored a positive value.</p>
 *
 * <h2>Why both members say {@code remap = false}</h2>
 *
 * <p>{@code setCurrentCartSpeedCapOnRail} and {@code currentSpeedCapOnRail} are <b>Forge additions</b>,
 * not vanilla members — they sit in the {@code // Forge Start} block of {@code AbstractMinecart}. They
 * are therefore absent from the SRG mappings the annotation processor reads, and leaving remapping on
 * fails the build outright ("Unable to locate obfuscation mapping for @Inject target"). Forge does not
 * rename its own additions, so the names are the same in dev and in production and no remap is
 * wanted.</p>
 */
@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartSpeedCapMixin {

    /** {@code AbstractMinecart.currentSpeedCapOnRail} — Forge's field, private in vanilla. */
    @Shadow(remap = false)
    private float currentSpeedCapOnRail;

    /**
     * Forge clamps the assignment to {@code getMaxCartSpeedOnRail()} (1.2 for a vanilla cart); this
     * stores the value the rail asked for instead. See the class javadoc for the scope.
     */
    @Inject(method = "setCurrentCartSpeedCapOnRail(F)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void gregtech$handTheRailItsOwnSpeed(float value, CallbackInfo info) {
        if (value > 0.0F) {
            currentSpeedCapOnRail = value;
            info.cancel();
        }
    }
}
