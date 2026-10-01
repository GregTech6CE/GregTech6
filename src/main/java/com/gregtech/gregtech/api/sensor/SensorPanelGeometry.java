package com.gregtech.gregtech.api.sensor;
import net.minecraft.core.Direction;
public final class SensorPanelGeometry {private SensorPanelGeometry(){}public static int[] bounds(Direction facing){return SensorPanelRules.bounds(facing.ordinal());}}
