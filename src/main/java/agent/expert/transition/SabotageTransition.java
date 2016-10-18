package agent.expert.transition;

import agent.expert.MCTSPlayer;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class SabotageTransition extends ResistanceTransition {

    private final boolean sabotaging;

    public SabotageTransition(MCTSPlayer player, Type type, boolean sabotaging) {
        super(player, type);
        this.sabotaging = sabotaging;
    }

    public boolean sabotaging() {
        return sabotaging;
    }

}
