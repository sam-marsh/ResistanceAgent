package agent;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Sam Marsh
 * Date: 11/10/2016
 */
public class Mission {

    private final Player leader;
    private final Set<Player> team;

    private int traitors;

    public Mission(GameState state, String _leader, String _team) {
        leader = state.lookup(_leader.charAt(0));
        team = new HashSet<Player>();
        for (char id : _team.toCharArray()) {
            team.add(state.lookup(id));
        }
        traitors = -1;
    }

    public boolean done() {
        return traitors != -1;
    }

    public void done(int traitors) {
        this.traitors = traitors;
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
