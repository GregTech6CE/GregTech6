package com.gregtech.gregtech.api.energy;
import net.minecraft.core.Direction;
/** Platform direction adapter for the shared original face masks. */
public record FaceConfig(int itemInputs,int itemOutputs,int fluidInputs,int fluidOutputs,
 int energyInputs,int energyOutputs,int itemAutoInput,int itemAutoOutput,int fluidAutoInput,int fluidAutoOutput) {
 public static final int BOTTOM=MachineFaceMasks.BOTTOM,TOP=MachineFaceMasks.TOP,LEFT=MachineFaceMasks.LEFT,
 RIGHT=MachineFaceMasks.RIGHT,FRONT=MachineFaceMasks.FRONT,BACK=MachineFaceMasks.BACK,AUTO_NONE=MachineFaceMasks.AUTO_NONE;
 public static FaceConfig from(MachineFaceMasks m) { return new FaceConfig(m.itemInputs(),m.itemOutputs(),m.fluidInputs(),m.fluidOutputs(),m.energyInputs(),m.energyOutputs(),m.itemAutoInput(),m.itemAutoOutput(),m.fluidAutoInput(),m.fluidAutoOutput()); }
 public static final FaceConfig ALL_SIDES=from(MachineFaceMasks.ALL_SIDES);
 public static final FaceConfig TOP_IN_BOTTOM_OUT=from(MachineFaceMasks.TOP_IN_BOTTOM_OUT);
 public static final FaceConfig ALL_IN_ALL_OUT=from(MachineFaceMasks.ALL_IN_ALL_OUT);
 public static final FaceConfig TOP_IN_BOTTOM_FLUID=from(MachineFaceMasks.TOP_IN_BOTTOM_FLUID);
 public static final FaceConfig NO_ENERGY=from(MachineFaceMasks.NO_ENERGY);
 public static boolean has(int mask,Direction d){return d!=null&&MachineFaceMasks.has(mask,d.get3DDataValue());}
 public static boolean has(int mask,int d){return MachineFaceMasks.has(mask,d);}
 public static boolean autoValid(int d){return MachineFaceMasks.autoValid(d);}
 public static Builder builder(){return new Builder();}
 public static class Builder {
  private final MachineFaceMasks.Builder delegate=MachineFaceMasks.builder();
  private static int[] sides(Direction[] ds){return java.util.Arrays.stream(ds).filter(java.util.Objects::nonNull).mapToInt(Direction::get3DDataValue).toArray();}
  public Builder itemIn(int... ds){delegate.itemIn(ds);return this;}
  public Builder itemIn(Direction... ds){return itemIn(sides(ds));}
  public Builder itemOut(int... ds){delegate.itemOut(ds);return this;}
  public Builder itemOut(Direction... ds){return itemOut(sides(ds));}
  public Builder fluidIn(int... ds){delegate.fluidIn(ds);return this;}
  public Builder fluidIn(Direction... ds){return fluidIn(sides(ds));}
  public Builder fluidOut(int... ds){delegate.fluidOut(ds);return this;}
  public Builder fluidOut(Direction... ds){return fluidOut(sides(ds));}
  public Builder energyIn(int... ds){delegate.energyIn(ds);return this;}
  public Builder energyIn(Direction... ds){return energyIn(sides(ds));}
  public Builder energyOut(int... ds){delegate.energyOut(ds);return this;}
  public Builder energyOut(Direction... ds){return energyOut(sides(ds));}
  public Builder itemAutoIn(int d){delegate.itemAutoIn(d);return this;}
  public Builder itemAutoIn(Direction d){return itemAutoIn(d==null?-1:d.get3DDataValue());}
  public Builder itemAutoOut(int d){delegate.itemAutoOut(d);return this;}
  public Builder itemAutoOut(Direction d){return itemAutoOut(d==null?-1:d.get3DDataValue());}
  public Builder fluidAutoIn(int d){delegate.fluidAutoIn(d);return this;}
  public Builder fluidAutoIn(Direction d){return fluidAutoIn(d==null?-1:d.get3DDataValue());}
  public Builder fluidAutoOut(int d){delegate.fluidAutoOut(d);return this;}
  public Builder fluidAutoOut(Direction d){return fluidAutoOut(d==null?-1:d.get3DDataValue());}
  public FaceConfig build(){return from(delegate.build());}
 }
}
