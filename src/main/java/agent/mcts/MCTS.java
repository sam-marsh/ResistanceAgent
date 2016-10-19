package agent.mcts;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Sam Marsh
 */
public class MCTS {

    private static final SelectionPolicy POLICY = SelectionPolicy.ROBUST_CHILD;

    private final ExecutorService executor;
    private volatile boolean searching;

    private State state;
    private Future<?> future;
    private Node root;

    public MCTS(State state) {
        this.state = state;
        this.executor = Executors.newSingleThreadExecutor();
        this.searching = false;
        this.future = null;
    }

    public void state(State state) {
        this.state = state;
    }

    public void search() {
        root = new Node(state);
        future = executor.submit(new Runnable() {
            @Override
            public void run() {
                searching = true;
                while (searching) {
                    select(state.copy(), root);
                }
            }
        });
    }

    public Transition transition() {
        searching = false;
        try {
            future.get();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        //TODO was getting an exception here caused by an empty child list? May have been fixed, but not sure...
        return POLICY.choice(root).transition();
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void select(State state, Node node) {
        Map.Entry<State, Node> pair = policy(state, node);
        int[] scores = simulate(pair.getKey());
        Node child = pair.getValue();
        child.backPropagate(scores);
    }

    private Map.Entry<State, Node> policy(State state, Node node) {
        while (!state.complete()) {
            if (!node.expanded()) {
                node.expand(state);
            }
            if (!node.unvisitedChildren().isEmpty()) {
                Node child = node.unvisitedChildren().remove((int) (Math.random() * node.unvisitedChildren().size()));
                node.addChild(child);
                state.transition(child.transition());
                return new AbstractMap.SimpleEntry<State, Node>(state, child);
            } else {
                List<Node> best = findChildren(node);
                if (best.isEmpty()) {
                    return new AbstractMap.SimpleEntry<State, Node>(state, node);
                }
                Node last = best.get((int) (Math.random() * best.size()));
                node = last;
                state.transition(last.transition());
            }
        }
        return new AbstractMap.SimpleEntry<State, Node>(state, node);
    }

    private List<Node> findChildren(Node node) {
        double max = Double.NEGATIVE_INFINITY;
        List<Node> list = new ArrayList<Node>();
        for (Node child : node.children()) {
            double ucb = child.ucb();
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

    private int[] simulate(State _state) {
        State state = _state.copy();
        while (!state.complete()) {
            List<Transition> transitions = state.transitions();
            Transition transition = transitions.get((int) (Math.random() * transitions.size()));
            state.transition(transition);
        }
        return state.scores();
    }

    public enum SelectionPolicy {

        MAX_CHILD {
            @Override
            public Node choice(Node node) {
                double max = Double.MIN_VALUE;
                List<Node> list = new ArrayList<Node>();
                for (Node child : node.children()) {
                    System.out.println(child);
                    double score = child.score(node.player());
                    if (score > max) {
                        list.clear();
                        max = score;
                        list.add(child);
                    } else if (score == max) {
                        list.add(child);
                    }
                }
                return list.get((int) (Math.random() * list.size()));
            }
        },

        ROBUST_CHILD {
            @Override
            public Node choice(Node node) {
                int max = Integer.MIN_VALUE;
                List<Node> list = new ArrayList<Node>();
                for (Node child : node.children()) {
                    int games = child.games();
                    if (games > max) {
                        list.clear();
                        max = games;
                        list.add(child);
                    } else if (games == max) {
                        list.add(child);
                    }
                }
                return list.get((int) (Math.random() * list.size()));

            }
        };

        public abstract Node choice(Node node);

    }

    public static class Node {

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

        public void backPropagate(int[] score) {
            this.games++;
            for (int i = 0; i < score.length; i++)
                this.score[i] += score[i];
            if (parent != null) {
                parent.backPropagate(score);
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

        @Override
        public String toString() {
            return String.format(
                    "Node{score=%s, games=%d, transition=%s, player=%d}",
                    Arrays.toString(score), games, transition, player
            );
        }

    }

    public interface State {

        State copy();

        List<Transition> transitions();

        void transition(Transition transition);

        boolean complete();

        int currentPlayer();

        int numPlayers();

        int[] scores();

    }

    public interface Transition {}

}
