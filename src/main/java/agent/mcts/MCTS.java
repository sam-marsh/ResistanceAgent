package agent.mcts;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

}
