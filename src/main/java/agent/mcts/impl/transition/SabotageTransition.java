package agent.mcts.impl.transition;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class SabotageTransition extends ResistanceTransition {

    private final boolean sabotage;

    public SabotageTransition(boolean sabotage) {
        this.sabotage = sabotage;
    }

    public boolean sabotage() {
        return sabotage;
    }

}
