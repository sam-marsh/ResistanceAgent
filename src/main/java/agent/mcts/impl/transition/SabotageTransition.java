package agent.mcts.impl.transition;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class SabotageTransition extends ResistanceTransition {

    private final int traitors;

    public SabotageTransition(boolean me, int traitors) {
        super(me);
        this.traitors = traitors;
    }

    public int traitors() {
        return traitors;
    }

}
