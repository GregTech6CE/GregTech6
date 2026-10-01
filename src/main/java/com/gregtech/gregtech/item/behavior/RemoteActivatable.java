package com.gregtech.gregtech.item.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * GT6 {@code ITileEntityRemoteActivateable}
 * ({@code gregapi/tileentity/ITileEntityRemoteActivateable.java:26-29}) — the block half of the
 * Remote Activator: one method that fires the block and reports whether the remote may keep it.
 *
 * <h2>GT6's contract: the return value is "keep me bound", not "it worked"</h2>
 *
 * <p>The interface's own javadoc states it ({@code :27}): "if it should stay inside the List of
 * activate-able things. False for things like Dynamite, that will vanish after use, for example."
 * The single caller is the remote itself, {@code Behavior_Remote.onItemRightClick}
 * ({@code Behavior_Remote.java:79-86}): it walks the coordinates bound to the held item, fires every
 * one of them that lies within {@code 128} blocks ({@code :80-82}) and collects the survivors into
 * {@code tToBeKept}. A coordinate is only added back when the block there is a
 * {@code ITileEntityRemoteActivateable} <em>and</em> {@code remoteActivate()} returned {@code T}
 * ({@code :82}); everything else — including a block that just fired and answered {@code F} — is
 * dropped from the item's NBT by the {@code setCoords} that follows ({@code :87}), so the next remote
 * click no longer knows about that coordinate. That is why the return value of a successful firing can
 * legitimately be {@code false}.</p>
 *
 * <h2>GT6's two implementers, and why they disagree</h2>
 *
 * <ul>
 *   <li>{@code MultiTileEntityButtonAdvanced} ({@code :54}) — the advanced button/lever. Its
 *       {@code remoteActivate()} ({@code :197-210}) toggles the block on the server side and returns
 *       {@code T} ({@code :209}), because the switch is still standing afterwards and should stay
 *       bound;</li>
 *   <li>{@code MultiTileEntityDynamite} ({@code :61}) — a placed charge. Its {@code remoteActivate()}
 *       ({@code :193}) returns {@code F} <em>deliberately</em>, while still arming the fuse
 *       ({@code mCountDown = 20}) and refreshing the client: firing a charge removes it from the
 *       remote, because the block is about to vanish. The same method is what the
 *       redstone/burning check calls at {@code :109}, where the arming is what matters and the return
 *       value is discarded.</li>
 * </ul>
 *
 * <h2>Why the port hands the position in</h2>
 *
 * <p>GT6's method takes no arguments because the block entity it is called on already knows its own
 * {@code xCoord}/{@code yCoord}/{@code zCoord}. The port's behaviours are {@code static} and take the
 * position as a parameter (see the class javadoc of {@link ItemBehaviors}), and an implementer may be
 * a plain block with no block entity at all, so {@link BehaviorRemote} passes in the coordinate it
 * resolved — always the coordinate the remote has bound, never a neighbour.</p>
 *
 * <p>Implementers in the port: the dynamite
 * ({@code com.gregtech.gregtech.block.tool.DynamiteBlock}, GT6 {@code MultiTileEntityDynamite:61})
 * detonates and returns {@code false}, GT6's own answer, so a fired charge leaves the remote's list;
 * nothing else implements it yet. {@link BehaviorRemote#useOn} refuses to bind a position whose block
 * does not implement this interface.</p>
 */
public interface RemoteActivatable {
    /** GT6 {@code ITileEntityRemoteActivateable#remoteActivate()}. @return GT6's boolean result */
    boolean remoteActivate(Level level, BlockPos pos);
}
