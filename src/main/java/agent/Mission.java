package agent;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Sam Marsh
 * Date: 11/10/2016
 */
public class Mission {

    private final char leader;
    private final Set<Character> team;

    private int traitors;

    public Mission(GameState state, String _leader, String _team) {
        leader = _leader.charAt(0);
        team = new HashSet<Character>();
        for (char id : _team.toCharArray()) {
            team.add(id);
        }
        traitors = -1;
    }

    public boolean done() {
        return traitors != -1;
    }

    public void undo() {
        this.traitors = -1;
    }

    public void done(int traitors) {
        this.traitors = traitors;
    }

    public int traitors() {
        return traitors;
    }

    public char leader() {
        return leader;
    }

    public Set<Character> team() {
        return team;
    }

    @Override
    public String toString() {
        return String.format("Mission{leader=%s, team=%s, done=%s, traitors=%d}", leader, team, done(), traitors);
    }

}
