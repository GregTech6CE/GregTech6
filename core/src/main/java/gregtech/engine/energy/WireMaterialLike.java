package gregtech.engine.energy;

/** Shared material metadata; loader blocks keep their own world/connection callbacks. */
public interface WireMaterialLike {
    WireSpec spec();
}
