package agent.mcts.impl.transition;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class VoteTransition extends ResistanceTransition {

    private final int votes;

    public VoteTransition(boolean me, int votes) {
        super(me);
        this.votes = votes;
    }

    public int votes() {
        return votes;
    }

}
