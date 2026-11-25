package gregtech.engine.energy;

import static gregtech.content.data.GregTechTags.Energy.CU;
import static gregtech.content.data.GregTechTags.Energy.EU;
import static gregtech.content.data.GregTechTags.Energy.RF;
import static gregtech.content.data.GregTechTags.Energy.RU;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gregtech.content.energy.CrystalChargerDefinitions;
import gregtech.content.energy.GearboxCatalog;
import gregtech.content.energy.OriginalAdjacentEnergyRules;
import gregtech.content.energy.OriginalEnergyDeviceTooltipData;
import gregtech.content.energy.OriginalRotaryConverter;
import gregtech.content.energy.OriginalThermalConverter;
import gregtech.content.material.MaterialTestFixture;
import gregtech.content.material.Materials;
import gregtech.engine.energy.EnergyNodeSpec.DeviceType;
import gregtech.engine.energy.EnergyNodeSpec.Kind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class EnergyNodeSpecTest {
    @BeforeAll
    static void initializeMaterials() {
        MaterialTestFixture.ensureInitialized();
    }

    @Test
    void explicitBehaviorWinsOverRegistryName() {
        var cooler = EnergyNodeSpec.builder("electric_motor_custom", Materials.Steel)
                .deviceType(DeviceType.COOLER)
                .texture("cooler")
                .input(RF, 128)
                .output(CU, 8)
                .name("RF Cooler")
                .build();
        assertFalse(OriginalRotaryConverter.motor(cooler));
        assertTrue(OriginalThermalConverter.cooler(cooler));
        assertFalse(OriginalThermalConverter.modeSelectable(cooler));
        assertTrue(OriginalAdjacentEnergyRules.respondsToAdjacent(cooler));
        assertEquals(
                "gt.lang.face.any.but.front.back",
                OriginalEnergyDeviceTooltipData.profile(cooler, false).inputFaceKey());
    }

    @Test
    void oldSpecificationsKeepTheirInputAndControlRules() {
        var motor = new EnergyNodeSpec(
                "electric_motor_example", Materials.Steel, Kind.CONVERTER, "motor", EU, RU, 32, 16, 64, "Motor", 0);
        assertEquals(DeviceType.MOTOR, motor.deviceType());
        assertTrue(OriginalRotaryConverter.motor(motor));

        var turbine = EnergyNodeSpec.builder("custom_turbine", Materials.Steel)
                .kind(Kind.TURBINE)
                .texture("turbine")
                .input(EU, 32)
                .output(RU, 16)
                .name("Turbine")
                .build();
        assertEquals(Kind.TURBINE, turbine.kind());
        assertEquals(DeviceType.BUFFERED, turbine.deviceType());
        assertFalse(OriginalAdjacentEnergyRules.respondsToAdjacent(turbine));
    }

    @Test
    void bufferedDevicesCanStoreEnergyButCannotAlsoActAsSolarPanels() {
        var spec = EnergyNodeSpec.builder("solar_example", Materials.Steel)
                .kind(Kind.SOLAR)
                .deviceType(DeviceType.BUFFERED)
                .texture("panel")
                .input(EU, 0)
                .output(EU, 8)
                .name("Panel");
        assertThrows(IllegalArgumentException.class, spec::build);
        assertEquals(DeviceType.BUFFERED, spec.kind(Kind.STORAGE).build().deviceType());
    }

    @Test
    void transformerPacketSizesSupportTheirConversionRatios() {
        for (var type : new DeviceType[] {DeviceType.ELECTRIC_TRANSFORMER, DeviceType.ROTATION_TRANSFORMER}) {
            var builder = EnergyNodeSpec.builder("test_transformer", Materials.Steel)
                    .deviceType(type)
                    .texture("test")
                    .name("Test Transformer");
            assertThrows(
                    IllegalArgumentException.class,
                    () -> builder.input(EU, 0).output(EU, 1).build());
            assertThrows(
                    IllegalArgumentException.class,
                    () -> builder.input(EU, 1).output(EU, 0).build());
            assertEquals(1, builder.input(EU, 1).output(EU, 1).build().inputRate());
        }
        var stepDown = EnergyNodeSpec.builder("transformer_example", Materials.Steel)
                .texture("test")
                .name("Legacy Electric Transformer")
                .input(EU, 1)
                .output(EU, 2);
        assertThrows(IllegalArgumentException.class, stepDown::build);
        assertEquals(
                DeviceType.ELECTRIC_TRANSFORMER, stepDown.input(EU, 2).build().deviceType());
    }

    @Test
    void familiesThatConvertOrDistributeInputRequireAPositiveRating() {
        for (var type : DeviceType.values()) {
            if (!type.usesOriginalConverter() && type != DeviceType.BATTERY_BOX) continue;
            var builder = EnergyNodeSpec.builder("test_device", Materials.Steel)
                    .deviceType(type)
                    .texture("test")
                    .name("Test Device")
                    .input(EU, 0)
                    .output(EU, 1);
            if (type == DeviceType.BATTERY_BOX) builder.batterySlots(4);
            assertThrows(IllegalArgumentException.class, builder::build);
        }
        assertEquals(
                0,
                EnergyNodeSpec.builder("test_solar", Materials.Steel)
                        .deviceType(DeviceType.SOLAR)
                        .texture("test")
                        .name("Test Panel")
                        .input(EU, 0)
                        .output(EU, 1)
                        .build()
                        .inputRate());
    }

    @Test
    void separatelyRegisteredDevicesShareTheAppropriateFamilies() {
        for (var tier : GearboxCatalog.tiers()) {
            assertEquals(
                    DeviceType.ROTATION_TRANSFORMER,
                    GearboxCatalog.transformer(tier).deviceType());
        }
        for (var charger : CrystalChargerDefinitions.specifications()) {
            assertEquals(DeviceType.BATTERY_BOX, charger.deviceType());
            assertEquals(Kind.STORAGE, charger.kind());
        }
    }
}
