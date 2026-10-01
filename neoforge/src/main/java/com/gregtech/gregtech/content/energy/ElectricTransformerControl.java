package com.gregtech.gregtech.content.energy;
import com.gregtech.gregtech.api.machine.MachineControl;
import net.minecraft.nbt.CompoundTag;
/** Native NBT/control interface over the single original activity/mode state machine. */
public final class ElectricTransformerControl extends TransformerControlState implements MachineControl {
 public ElectricTransformerControl(Runnable changed){super(changed);}
 public void save(CompoundTag tag){var s=snapshot();tag.putBoolean("gt.transformer_stopped",!s.enabled());tag.putInt("gt.transformer_mode",s.mode());tag.putBoolean("gt.transformer_possible",s.possible());tag.putBoolean("gt.transformer_emitted",s.emitted());}
 public void load(CompoundTag tag){restore(new Snapshot(!tag.getBoolean("gt.transformer_stopped"),tag.getInt("gt.transformer_mode"),tag.getBoolean("gt.transformer_possible"),tag.getBoolean("gt.transformer_emitted")),tag.getBoolean("gt.inverted"));}
}
