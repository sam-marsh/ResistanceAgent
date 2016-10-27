package agent.mcts;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Sam Marsh
 */
public class MCTS {

    /**
     * The selection expand for the root child. For The Resistance, {@link SelectionPolicy#MAX_CHILD} works much
     * better than {@link SelectionPolicy#ROBUST_CHILD} (from experimentation).
     * TODO check this after integrated opponent model
     */
    private static final SelectionPolicy POLICY = SelectionPolicy.MAX_CHILD;

    /**
     * The random number generator used for random simulations, etc.
     */
    private static final Random RANDOM = new Random();

    /**
     * The thread which does the searching.
     */
    private final ExecutorService executor;

    /**
     * Whether a search is in progress. Volatile since it needs to be accessed from two threads.
     * TODO check whether anything else needs to be done apart from making this volatile (???)
     */
    private volatile boolean searching;

    /**
     * The initial state of the game.
     */
    private State state;

    /**
     * The currently executing search.
     */
    private Future<?> future;

    /**
     * The root of the search tree.
     */
    private Node root;

    /**
     * Creates a new Monte Carlo Search tree from the given state.
     *
     * @param state the state to start searching from
     */
    public MCTS(State state) {
        this.state = state;
        this.executor = Executors.newSingleThreadExecutor();
        this.searching = false;
        this.future = null;
    }

    /**
     * Updates the initial state. Cannot be called while searching.
     *
     * @param state the new state to search from
     * @throws IllegalStateException if a search is in progress
     */
    public void state(State state) throws IllegalStateException {
        if (searching || (future != null && !future.isDone())) {
            transition();
        }
        this.state = state.copy();
    }

    /**
     * Begins the asynchronous search and returns immediately.
     */
    public void search() {
        root = new Node(state);
        future = executor.submit(new Runnable() {
            @Override
            public void run() {
                searching = true;
                //continue to sample until the user tells us to stop
                while (searching) {
                    select(state.copy(), root);
                }
            }
        });
    }

    /**
     * Finishes the search and returns the optimal move choice.
     *
     * @return the optimal transition to take from the root
     */
    public Transition transition() {
        searching = false;
        try {
            //wait until loop finishes
            future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        //get the best child according to the root child selection expand
        return POLICY.choice(root).transition;
    }

    /**
     * Shuts down the executor.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * Step one: selection. Starting at the root, the selection expand is applied recursively
     * to move through the tree structure.
     *
     * @param state the current state
     * @param node the node to select from
     */
    private void select(State state, Node node) {
        Result pair = expand(state, node);
        int[] scores = simulate(pair.state);
        pair.node.backPropagate(scores);
    }

    /**
     * Step two: expansion. Child node added to expand the tree.
     *
     * @param state the current game state
     * @param node the node to expand from
     * @return the new node with the corresponding state
     */
    private Result expand(State state, Node node) {
        //continue to loop until reach end
        while (!state.complete()) {
            //expand the node
            if (!node.expanded()) {
                node.expand(state);
            }
            if (!node.unvisited.isEmpty()) {
                //choose a random unvisited child node, add to list of children for that node, transition into node
                Node child = node.unvisited.remove(RANDOM.nextInt(node.unvisited.size()));
                node.children.add(child);
                //change state based on this node's transition
                state.transition(child.transition);
                return new Result(state, child);
            } else {
                //visited all children of this node: so pick the best one
                List<Node> best = findChildren(node);
                if (best.isEmpty()) {
                    //no choices at all - return what was passed in
                    return new Result(state, node);
                }
                node = randomChoice(best);
                //change state based on this node's transition
                state.transition(node.transition);
            }
        }
        return new Result(state, node);
    }

    /**
     * Exploration/exploitation: uses the UCT method to pick the best
     * child node(s). This returns a list and not a single node because
     * multiple children may be equal.
     *
     * @param node the node from which a child will be picked
     * @return the best nodes to look at next
     */
    private List<Node> findChildren(Node node) {
        double max = Double.NEGATIVE_INFINITY;
        List<Node> list = new ArrayList<Node>();
        for (Node child : node.children) {
            double ucb = child.ucb1();
            if (ucb > max) {
                list.clear();
                list.add(child);
                max = ucb;
            } else if (ucb == max) {
                list.add(child);
            }
        }
        return list;
    }

    /**
     * Step three: simulation. The game is played out from the given state to produce a final result.
     *
     * @param _state the state to simulate from
     * @return the final scores
     */
    private int[] simulate(State _state) {
        State state = _state.copy();
        //keep looping until game complete
        while (!state.complete()) {
            //pick a random transition and update state by taking that transition
            Map<Transition, Double> transitions = state.weightedTransitions();
            Transition transition = randomChoice(transitions);
            state.transition(transition);
        }
        return state.scores();
    }

    private <T> T randomChoice(Map<T, Double> weightedMap) {
        double totalWeight = 0;
        T selected = null;
        for (Map.Entry<T, Double> entry : weightedMap.entrySet()) {
            double weight = entry.getValue();
            double r = RANDOM.nextDouble() * (totalWeight + weight);
            if (r >= totalWeight) {
                selected = entry.getKey();
            }
            totalWeight += weight;
        }
        return selected;
    }

    /**
     * The selection expand for the root children.
     */
    private enum SelectionPolicy {

        /**
         * Take the node with the highest score.
         */
        MAX_CHILD {
            @Override
            public Node choice(Node node) {
                int max = Integer.MIN_VALUE;
                List<Node> list = new ArrayList<Node>();
                for (Node child : node.children) {
                    max = updateMaximum(child, list, child.score[node.player], max);
                }
                return randomChoice(list);
            }
        },

        /**
         * Take the node with the highest visit count.
         */
        ROBUST_CHILD {
            @Override
            public Node choice(Node node) {
                int max = Integer.MIN_VALUE;
                List<Node> list = new ArrayList<Node>();
                for (Node child : node.children) {
                    max = updateMaximum(child, list, child.games, max);
                }
                return randomChoice(list);

            }
        };

        /**
         * Given a node, chooses a child node based on this expand.
         *
         * @param node the node to randomChoice children from
         * @return the 'best' child according to the expand
         */
        public abstract Node choice(Node node);

    }

    /**
     * Convenience method since this is used often in the MCTS algorithm. Checks if a node is better than or
     * as good as the current best node, if so adds it to the list and returns the new max value.
     *
     * @param node the node to check
     * @param list the list containing the maximum nodes
     * @param value the node's value to check
     * @param max the current maximum
     * @return the new maximum
     */
    private static int updateMaximum(Node node, List<Node> list, int value, int max) {
        if (value > max) {
            list.clear();
            max = value;
            list.add(node);
        } else if (value == max) {
            list.add(node);
        }
        return max;
    }

    private static <T> T randomChoice(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    /**
     * Represents a node in the game tree.
     */
    private static class Node {

        //holds each player's score (in The Resistance, simply 0=on losing team and 1=winning team)
        private int[] score;
        //the number of games carried out in this subtree
        private int games;
        //the transition performed on the previous state
        private Transition transition;
        //this node's children which have not been visited yet
        private List<Node> unvisited;
        //the children which HAVE been visited
        private List<Node> children;
        //the node above us in the tree
        private Node parent;
        //the current player at this node (c.f. minimax)
        private int player;

        /**
         * Creates the root node.
         *
         * @param state the initial state
         */
        Node(State state) {
            this.children = new ArrayList<Node>();
            this.player = state.currentPlayer();
            this.score = new int[state.numPlayers()];
        }

        /**
         * Creates a node from acting on a state with a transition, having the given parent node.
         *
         * @param state the 'parent' state
         * @param transition the transition to perform
         * @param parent the node's parent
         */
        Node(State state, Transition transition, Node parent) {
            this.children = new ArrayList<Node>();
            this.parent = parent;
            this.transition = transition;
            State copy = state.copy();
            copy.transition(transition);
            this.player = copy.currentPlayer();
            this.score = new int[state.numPlayers()];
        }

        /**
         * @return whether this node has been expanded yet
         */
        boolean expanded() {
            return unvisited != null;
        }

        /**
         * Calculates the UCB1 formula for the node.
         * See https://en.wikipedia.org/wiki/Monte_Carlo_tree_search#Exploration_and_exploitation
         *
         * @return the UCB1 value
         */
        double ucb1() {
            return (double) score[parent.player] / games + Math.sqrt(2 * Math.log(parent.games + 1) / games);
        }

        /**
         * Back-propagates a score all the way up the tree.
         *
         * @param score the array of player scores
         */
        void backPropagate(int[] score) {
            this.games++;
            for (int i = 0; i < score.length; i++)
                this.score[i] += score[i];
            if (parent != null) {
                parent.backPropagate(score);
            }
        }

        /**
         * Expands this node.
         *
         * @param state the state to expand from
         */
        void expand(State state) {
            List<Transition> transitions = state.transitions();
            unvisited = new ArrayList<Node>();
            //for each possible transition, create a new child by acting on the state with that transition
            // and then add them to the unvisited children list
            for (Transition transition : transitions) {
                Node tempState = new Node(state, transition, this);
                unvisited.add(tempState);
            }
        }

    }

    /**
     * Represents the state of the game at a given time.
     */
    public interface State {

        /**
         * @return an exact copy of the state: actions performed on this copy should not affect the original in any way
         */
        State copy();

        /**
         * @return the transitions possible from this state
         */
        List<Transition> transitions();

        Map<Transition, Double> weightedTransitions();

        /**
         * Modifies this state by performing the given transition.
         *
         * @param transition the transition to carry out
         */
        void transition(Transition transition);

        /**
         * @return whether the game is over
         */
        boolean complete();

        /**
         * @return the current player - should be an integer ranging from 0 upwards
         */
        int currentPlayer();

        /**
         * @return the total number of players in the game
         */
        int numPlayers();

        /**
         * @return the scores for each player (index 0 holds score for player 0, etc)
         */
        int[] scores();

    }

    /**
     * A marker interface that represents a game transition.
     */
    public interface Transition {}

    /**
     * A convenience class for holding a node with an associated state.
     */
    private class Result {

        private final State state;
        private final Node node;

        Result(State state, Node node) {
            this.state = state;
            this.node = node;
        }

    }

}
