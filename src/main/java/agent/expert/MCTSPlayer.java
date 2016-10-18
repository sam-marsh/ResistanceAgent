package agent.expert;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class MCTSPlayer {

    private final char id;
    private final boolean spy;

    public MCTSPlayer(char id, boolean spy) {
        this.id = id;
        this.spy = spy;
    }

    public char id() {
        return id;
    }

    public boolean spy() {
        return spy;
    }

}
