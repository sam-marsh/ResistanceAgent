package agent.mcts.impl.transition;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class SelectionTransition extends ResistanceTransition {

    private String selection;

    public SelectionTransition(boolean me, String selection) {
        super(me);
        this.selection = selection;
    }

    public String selection() {
        return selection;
    }

}
