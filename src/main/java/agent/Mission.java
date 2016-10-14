package agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Author: Sam Marsh
 * Date: 11/10/2016
 */
public class Mission {

    private final Player leader;
    private final Set<Player> team;
    private final Map<Player, Double> originalSpyProbabilities;

    private int traitors;

    public Mission(GameState state, String _leader, String _team) {
        leader = state.lookup(_leader.charAt(0));
        team = new HashSet<Player>();
        for (char id : _team.toCharArray()) {
            team.add(state.lookup(id));
        }
        traitors = -1;
        originalSpyProbabilities = new HashMap<Player, Double>(state.players().size());
        for (Player p : state.players())
            originalSpyProbabilities.put(p, p.bayesSuspicion());
    }

    public boolean done() {
        return traitors != -1;
    }

    public void done(int traitors) {
        this.traitors = traitors;
    }

    public Map<Player, Double> originalProbabilities() {
        return originalSpyProbabilities;
    }

    public int traitors() {
        return traitors;
    }

    public Player leader() {
        return leader;
    }

    public Set<Player> team() {
        return team;
    }

    @Override
    public String toString() {
        return "Mission{" +
                "leader=" + leader +
                ", team=" + team +
                ", done=" + done() +
                ", traitors=" + traitors +
                '}';
    }

}
