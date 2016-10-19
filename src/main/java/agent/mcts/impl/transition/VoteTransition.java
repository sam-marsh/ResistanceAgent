package agent.mcts.impl.transition;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class VoteTransition extends ResistanceTransition {

    private final boolean yes;

    public VoteTransition(boolean yes) {
        this.yes = yes;
    }

    public boolean yes() {
        return yes;
    }

}
