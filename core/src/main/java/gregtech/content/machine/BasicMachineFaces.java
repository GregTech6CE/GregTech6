/* GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later. */
package gregtech.content.machine;

import static gregtech.api.machine.MachineFace.BACK;
import static gregtech.api.machine.MachineFace.BOTTOM;
import static gregtech.api.machine.MachineFace.FRONT;
import static gregtech.api.machine.MachineFace.LEFT;
import static gregtech.api.machine.MachineFace.RIGHT;
import static gregtech.api.machine.MachineFace.TOP;

import gregtech.api.machine.MachineFace;
import gregtech.api.machine.MachineFaceMasks;

/** Machine-relative I/O layouts from the GT6 {@code Loader_MultiTileEntities} registrations. */
final class BasicMachineFaces {
    private static final MachineFace[] ALL_FACES = MachineFace.values();

    private BasicMachineFaces() {}

    static final MachineFaceMasks ANALYZER = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(BOTTOM, RIGHT)
            .fluidIn(BOTTOM, TOP)
            .fluidOut(ALL_FACES)
            .energyIn(BACK)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(TOP)
            .build();

    static final MachineFaceMasks AUTOCLAVE = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(RIGHT, BACK)
            .fluidIn(BOTTOM, LEFT)
            .fluidOut(RIGHT, BACK)
            .energyIn(ALL_FACES)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(BOTTOM)
            .fluidAutoOut(BACK)
            .build();

    static final MachineFaceMasks AUTOCRAFTER = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(BOTTOM, RIGHT)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(BOTTOM, TOP)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .build();

    static final MachineFaceMasks BATH = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(BOTTOM, RIGHT)
            .fluidIn(TOP, LEFT)
            .fluidOut(BOTTOM, RIGHT)
            .energyIn(ALL_FACES)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(TOP)
            .fluidAutoOut(BOTTOM)
            .build();

    static final MachineFaceMasks BUZZSAW = MachineFaceMasks.builder()
            .itemIn(LEFT)
            .itemOut(RIGHT)
            .fluidIn(BOTTOM, TOP)
            .fluidOut(ALL_FACES)
            .energyIn(BACK)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(BOTTOM)
            .build();

    static final MachineFaceMasks CANNER = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(BOTTOM, RIGHT)
            .fluidIn(TOP, LEFT)
            .fluidOut(BOTTOM, RIGHT)
            .energyIn(BACK)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(TOP)
            .fluidAutoOut(BOTTOM)
            .build();

    static final MachineFaceMasks CENTRIFUGE = MachineFaceMasks.builder()
            .itemIn(TOP)
            .itemOut(RIGHT)
            .fluidIn(TOP)
            .fluidOut(LEFT)
            .energyIn(BOTTOM)
            .itemAutoIn(TOP)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(TOP)
            .fluidAutoOut(LEFT)
            .build();

    static final MachineFaceMasks COAGULATOR = MachineFaceMasks.builder()
            .itemIn(ALL_FACES)
            .itemOut(BOTTOM, RIGHT)
            .fluidIn(TOP, LEFT)
            .fluidOut(ALL_FACES)
            .energyIn(ALL_FACES)
            .itemAutoOut(BOTTOM)
            .fluidAutoIn(TOP)
            .build();

    static final MachineFaceMasks COKE_OVEN = MachineFaceMasks.builder()
            .itemIn(TOP)
            .itemOut(BOTTOM)
            .fluidOut(BOTTOM, RIGHT, FRONT, LEFT, BACK)
            .energyIn(ALL_FACES)
            .fluidAutoOut(BOTTOM)
            .build();

    static final MachineFaceMasks CRACKER = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(RIGHT, BACK)
            .fluidIn(TOP, LEFT)
            .fluidOut(RIGHT, BACK)
            .energyIn(BOTTOM)
            .itemAutoIn(TOP)
            .itemAutoOut(BACK)
            .fluidAutoIn(LEFT)
            .fluidAutoOut(RIGHT)
            .build();

    static final MachineFaceMasks CRYSTALLISATION_CRUCIBLE = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT, BACK)
            .itemOut(RIGHT)
            .fluidIn(TOP, LEFT, BACK)
            .fluidOut(ALL_FACES)
            .energyIn(BOTTOM)
            .itemAutoIn(TOP)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(LEFT)
            .build();

    static final MachineFaceMasks DEBARKER = MachineFaceMasks.builder()
            .itemIn(LEFT)
            .itemOut(RIGHT)
            .fluidIn(BOTTOM, TOP)
            .fluidOut(ALL_FACES)
            .energyIn(BACK)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(TOP)
            .build();

    static final MachineFaceMasks DISTILLATION_TOWER = MachineFaceMasks.builder()
            .itemIn(ALL_FACES)
            .itemOut(ALL_FACES)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(ALL_FACES)
            .itemAutoOut(BACK)
            .fluidAutoOut(BACK)
            .build();

    static final MachineFaceMasks DISTILLERY = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(RIGHT)
            .fluidIn(TOP, LEFT)
            .fluidOut(BACK)
            .energyIn(BOTTOM)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(TOP)
            .fluidAutoOut(BACK)
            .build();

    static final MachineFaceMasks ELECTROLYZER = MachineFaceMasks.builder()
            .itemIn(TOP, FRONT, BACK)
            .itemOut(RIGHT, LEFT)
            .fluidIn(TOP, FRONT, BACK)
            .fluidOut(RIGHT, LEFT)
            .energyIn(BOTTOM)
            .itemAutoIn(TOP)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(TOP)
            .fluidAutoOut(LEFT)
            .build();

    static final MachineFaceMasks LASER_WELDER = MachineFaceMasks.builder()
            .itemIn(LEFT)
            .itemOut(RIGHT)
            .fluidIn(BOTTOM, LEFT)
            .fluidOut(ALL_FACES)
            .energyIn(TOP)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(BOTTOM)
            .build();

    static final MachineFaceMasks LEFT_TO_RIGHT_BACK_POWER = MachineFaceMasks.builder()
            .itemIn(LEFT)
            .itemOut(RIGHT)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(BACK)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .build();

    static final MachineFaceMasks LEFT_TO_RIGHT_BOTTOM_POWER = MachineFaceMasks.builder()
            .itemIn(LEFT)
            .itemOut(RIGHT)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(BOTTOM)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .build();

    static final MachineFaceMasks LEFT_TO_RIGHT_TOP_POWER = MachineFaceMasks.builder()
            .itemIn(LEFT)
            .itemOut(RIGHT)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(TOP)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .build();

    static final MachineFaceMasks MAGNETIC_SEPARATOR = MachineFaceMasks.builder()
            .itemIn(LEFT)
            .itemOut(BOTTOM, RIGHT)
            .fluidIn(LEFT)
            .fluidOut(BOTTOM, RIGHT)
            .energyIn(TOP)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(LEFT)
            .fluidAutoOut(BOTTOM)
            .build();

    static final MachineFaceMasks MIXER = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(RIGHT, BACK)
            .fluidIn(TOP, LEFT)
            .fluidOut(RIGHT, BACK)
            .energyIn(BOTTOM)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(TOP)
            .fluidAutoOut(BACK)
            .build();

    static final MachineFaceMasks POLARIZER = MachineFaceMasks.builder()
            .itemIn(LEFT)
            .itemOut(RIGHT)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(BOTTOM, TOP)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .build();

    static final MachineFaceMasks PRINTER = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(BOTTOM, RIGHT)
            .fluidIn(TOP, LEFT)
            .fluidOut(ALL_FACES)
            .energyIn(BACK)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(TOP)
            .build();

    static final MachineFaceMasks ROASTER = MachineFaceMasks.builder()
            .itemIn(LEFT, BACK)
            .itemOut(RIGHT)
            .fluidIn(LEFT, BACK)
            .fluidOut(TOP)
            .energyIn(BOTTOM)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoIn(BACK)
            .fluidAutoOut(TOP)
            .build();

    static final MachineFaceMasks SCANNER = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(BOTTOM, RIGHT)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(BACK)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .build();

    static final MachineFaceMasks SMELTER = MachineFaceMasks.builder()
            .itemIn(TOP)
            .itemOut(LEFT)
            .fluidIn(TOP)
            .fluidOut(RIGHT)
            .energyIn(BOTTOM)
            .itemAutoIn(TOP)
            .itemAutoOut(LEFT)
            .fluidAutoIn(TOP)
            .fluidAutoOut(RIGHT)
            .build();

    static final MachineFaceMasks SQUEEZER = MachineFaceMasks.builder()
            .itemIn(LEFT)
            .itemOut(RIGHT)
            .fluidIn(ALL_FACES)
            .fluidOut(BOTTOM)
            .energyIn(TOP)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .fluidAutoOut(BOTTOM)
            .build();

    static final MachineFaceMasks TOP_LEFT_TO_RIGHT_BOTTOM_POWER = MachineFaceMasks.builder()
            .itemIn(TOP, LEFT)
            .itemOut(RIGHT)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(BOTTOM)
            .itemAutoIn(LEFT)
            .itemAutoOut(RIGHT)
            .build();

    static final MachineFaceMasks TOP_TO_BOTTOM_BACK_POWER = MachineFaceMasks.builder()
            .itemIn(TOP)
            .itemOut(BOTTOM)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(BACK)
            .itemAutoIn(TOP)
            .itemAutoOut(BOTTOM)
            .build();

    static final MachineFaceMasks TOP_TO_BOTTOM_LEFT_POWER = MachineFaceMasks.builder()
            .itemIn(TOP)
            .itemOut(BOTTOM)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(LEFT)
            .itemAutoIn(TOP)
            .itemAutoOut(BOTTOM)
            .build();

    static final MachineFaceMasks TOP_TO_BOTTOM_SIDE_POWER = MachineFaceMasks.builder()
            .itemIn(TOP)
            .itemOut(BOTTOM)
            .fluidIn(ALL_FACES)
            .fluidOut(ALL_FACES)
            .energyIn(RIGHT, LEFT)
            .itemAutoIn(TOP)
            .itemAutoOut(BOTTOM)
            .build();
}
