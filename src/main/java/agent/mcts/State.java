package agent.mcts;

import java.util.List;

/**
 * @author Sam Marsh
 */
public interface State {

    State copy();

    List<Transition> transitions();

    void transition(Transition transition);

    boolean complete();

    int currentPlayer();

    int numPlayers();

    int[] scores();

}
