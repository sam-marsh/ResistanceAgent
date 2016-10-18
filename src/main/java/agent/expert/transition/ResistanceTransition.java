package agent.expert.transition;

import agent.expert.MCTSPlayer;
import fr.avianey.mcts4j.Transition;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public abstract class ResistanceTransition implements Transition {

    private final MCTSPlayer player;

    public ResistanceTransition(MCTSPlayer player) {
        this.player = player;
    }

    public MCTSPlayer player() {
        return player;
    }

}
