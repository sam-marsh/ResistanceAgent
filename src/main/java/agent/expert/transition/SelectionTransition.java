package agent.expert.transition;

import agent.expert.MCTSPlayer;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class SelectionTransition extends ResistanceTransition {

    private String selection;

    public SelectionTransition(MCTSPlayer player, String selection) {
        super(player);
        this.selection = selection;
    }

    public String selection() {
        return selection;
    }

}
