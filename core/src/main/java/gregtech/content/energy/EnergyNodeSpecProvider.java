package gregtech.content.energy;

import gregtech.engine.energy.EnergyNodeSpec;
import gregtech.engine.energy.EnergyNodeSpec.DeviceType;

/** Device classification shared by registered blocks and their placed block entities. */
public interface EnergyNodeSpecProvider {
    /** A placed block entity can be queried before a specification has been bound. */
    EnergyNodeSpec spec();

    default boolean isOriginalRotaryConverter() {
        var spec = spec();
        return spec != null && OriginalRotaryConverter.handles(spec);
    }

    default boolean isOriginalThermalConverter() {
        var spec = spec();
        return spec != null && OriginalThermalConverter.handles(spec);
    }

    default boolean isOriginalMotor() {
        var spec = spec();
        return spec != null && OriginalRotaryConverter.motorBehavior(spec);
    }

    default boolean isMagnet() {
        var spec = spec();
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.MAGNET;
    }

    default boolean isTurbine() {
        var spec = spec();
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.TURBINE;
    }

    default boolean isInvertible() {
        var spec = spec();
        return spec != null && spec.kind() == EnergyNodeSpec.Kind.CONVERTER && spec.inType() == spec.outType();
    }

    default boolean isElectricTransformer() {
        var spec = spec();
        return spec != null && spec.deviceType() == DeviceType.ELECTRIC_TRANSFORMER;
    }

    default boolean isRotationTransformer() {
        var spec = spec();
        return spec != null && spec.deviceType() == DeviceType.ROTATION_TRANSFORMER;
    }

    default boolean isTransformer() {
        return isRotationTransformer() || isElectricTransformer();
    }
}
