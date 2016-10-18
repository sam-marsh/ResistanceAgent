package agent.expert.transition;

import agent.expert.MCTSPlayer;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class VoteTransition extends ResistanceTransition {

    private final boolean vote;

    public VoteTransition(MCTSPlayer player, boolean vote) {
        super(player);
        this.vote = vote;
    }

    public boolean vote() {
        return vote;
    }

}
