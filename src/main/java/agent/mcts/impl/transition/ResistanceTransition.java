package agent.mcts.impl.transition;

import agent.mcts.Transition;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public abstract class ResistanceTransition implements Transition {

    private final boolean me;

    public ResistanceTransition(boolean me) {
        this.me = me;
    }

    public boolean me() {
        return me;
    }

}
