package gregtech.content.transport;

/** Automatic connections for an item or fluid pipe face that is free to connect. */
public final class PipeConnectionRules {
    private PipeConnectionRules() {}

    public enum Neighbor {
        NONE,
        LOCKED_PIPE,
        CLOSED_PIPE,
        OPEN_PIPE,
        DECLARED_ENDPOINT,
        CAPABILITY_ENDPOINT
    }

    public enum Action {
        NONE,
        OPEN_LOCAL,
        OPEN_BOTH
    }

    /** Only the placement click can start a new pipe pair or connect a plain capability. */
    public static Action decide(boolean clickedSide, Neighbor neighbor) {
        return switch (neighbor) {
            case NONE, LOCKED_PIPE -> Action.NONE;
            case CLOSED_PIPE -> clickedSide ? Action.OPEN_BOTH : Action.NONE;
            case OPEN_PIPE -> clickedSide ? Action.OPEN_BOTH : Action.OPEN_LOCAL;
            case DECLARED_ENDPOINT -> Action.OPEN_LOCAL;
            case CAPABILITY_ENDPOINT -> clickedSide ? Action.OPEN_LOCAL : Action.NONE;
        };
    }
}
