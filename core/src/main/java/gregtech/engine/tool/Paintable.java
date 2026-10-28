package gregtech.engine.tool;

/** Brokestar IPaintableTE primitive contract, with platform persistence and sync outside core. */
public interface Paintable {
    boolean paint(int rgb);

    boolean mixPaint(int rgb);

    boolean unpaint();

    boolean isPainted();

    int getPaint();
}
