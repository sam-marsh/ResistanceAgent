package agent.mcts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Sam Marsh
 */
public class Node {

    private static final double EXPLORATION_CONSTANT = Math.sqrt(2);

    private int[] score;
    private int games;
    private Transition transition;
    private List<Node> unvisitedChildren;
    private List<Node> children;
    private Node parent;
    private int player;

    public Node(State state) {
        this.children = new ArrayList<Node>();
        this.player = state.currentPlayer();
        this.score = new int[state.numPlayers()];
    }

    public Node(State state, Transition transition, Node parent) {
        this.children = new ArrayList<Node>();
        this.parent = parent;
        this.transition = transition;
        State copy = state.copy();
        copy.transition(transition);
        this.player = copy.currentPlayer();
        this.score = new int[state.numPlayers()];
    }

    public int player() {
        return player;
    }

    public int games() {
        return games;
    }

    public int score(int player) {
        return score[player];
    }

    public Transition transition() {
        return transition;
    }

    public void addChild(Node child) {
        children.add(child);
    }

    public List<Node> children() {
        return children;
    }

    public List<Node> unvisitedChildren() {
        return unvisitedChildren;
    }

    public boolean expanded() {
        return unvisitedChildren != null;
    }

    public double ucb() {
        return (double) score[parent.player] / games + EXPLORATION_CONSTANT * Math.sqrt(Math.log(parent.games + 1) / games);
    }

    public void backpropagate(int[] score) {
        this.games++;
        for (int i = 0; i < score.length; i++)
            this.score[i] += score[i];
        if (parent != null) {
            parent.backpropagate(score);
        }
    }

    public void expand(State state) {
        List<Transition> transitions = state.transitions();
        unvisitedChildren = new ArrayList<Node>();
        for (Transition transition : transitions) {
            Node tempState = new Node(state, transition, this);
            unvisitedChildren.add(tempState);
        }
    }

}
