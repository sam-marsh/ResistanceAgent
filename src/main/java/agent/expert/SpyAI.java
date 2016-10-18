package agent.expert;

import agent.expert.transition.ResistanceTransition;
import agent.expert.transition.SabotageTransition;
import fr.avianey.mcts4j.DefaultNode;
import fr.avianey.mcts4j.Node;
import fr.avianey.mcts4j.UCT;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class SpyAI extends UCT<ResistanceTransition, DefaultNode<ResistanceTransition>> {

    private final SpyGameState state;

    public SpyAI(SpyGameState state) {
        this.state = state;
    }

    @Override
    public ResistanceTransition simulationTransition(Set<ResistanceTransition> possibleTransitions) {
        return choice(possibleTransitions);
    }

    @Override
    public ResistanceTransition expansionTransition(Set<ResistanceTransition> possibleTransitions) {
        return choice(possibleTransitions);
    }

    @Override
    protected void makeTransition(ResistanceTransition transition) {
        if (transition instanceof SabotageTransition) {
            boolean sabotaging = ((SabotageTransition) transition).sabotaging();
        }
    }

    @Override
    protected void unmakeTransition(ResistanceTransition transition) {

    }

    @Override
    public Set<ResistanceTransition> getPossibleTransitions() {
        return null;
    }

    @Override
    public DefaultNode<ResistanceTransition> newNode(Node<ResistanceTransition> parent, boolean terminal) {
        return null;
    }

    @Override
    public boolean isOver() {
        return false;
    }

    @Override
    public int getWinner() {
        return 0;
    }

    @Override
    public int getCurrentPlayer() {
        return 0;
    }

    @Override
    public void next() {

    }

    @Override
    public void previous() {

    }

    private <T> T choice(Set<T> set) {
        return new ArrayList<T>(set).get((int) (Math.random() * set.size()));
    }

}
