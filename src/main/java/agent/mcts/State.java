package agent.mcts;

import java.util.*;

/**
 * Author: Sam Marsh
 * Date: 14/10/2016
 */
public class State implements Cloneable {

    public static final int[][] MISSION_NUM = {
            { 2, 3, 2, 3, 3 },
            { 2, 3, 4, 3, 4 },
            { 2, 3, 3, 4, 4 },
            { 3, 4, 4, 5, 5 },
            { 3, 4, 4, 5, 5 },
            { 3, 4, 4, 5, 5 }
    };

    public Map<Character,SimulationAgent> players;
    public Set<Character> spies;
    public String playerString = "";
    public String spyString = "";
    public String resString = "";
    public int numPlayers = 0;
    public Random rand;
    public int fails;
    public int round;
    public int voteRound;
    public int leader;
    public String team;
    public Phase phase;

    public State() {

    }

    public State(State state) {
        this.players = new HashMap<Character, SimulationAgent>(state.players.size());
        for (Map.Entry<Character, SimulationAgent> entry : state.players.entrySet()) {
            this.players.put(entry.getKey(), entry.getValue().copy());
        }
        this.spies = new HashSet<Character>(state.spies);
        this.playerString = state.playerString;
        this.spyString = state.spyString;
        this.resString = state.resString;
        this.numPlayers = state.numPlayers;
        this.rand = state.rand;
        this.fails = state.fails;
        this.round = state.round;
        this.voteRound = state.voteRound;
        this.leader = state.leader;
        this.team = state.team;
        this.phase = state.phase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

        if (numPlayers != state.numPlayers) return false;
        if (fails != state.fails) return false;
        if (round != state.round) return false;
        if (voteRound != state.voteRound) return false;
        if (leader != state.leader) return false;
        if (players != null ? !players.equals(state.players) : state.players != null) return false;
        if (spies != null ? !spies.equals(state.spies) : state.spies != null) return false;
        if (playerString != null ? !playerString.equals(state.playerString) : state.playerString != null) return false;
        if (spyString != null ? !spyString.equals(state.spyString) : state.spyString != null) return false;
        if (resString != null ? !resString.equals(state.resString) : state.resString != null) return false;
        if (rand != null ? !rand.equals(state.rand) : state.rand != null) return false;
        if (team != null ? !team.equals(state.team) : state.team != null) return false;
        return phase == state.phase;

    }

    @Override
    public int hashCode() {
        int result = players != null ? players.hashCode() : 0;
        result = 31 * result + (spies != null ? spies.hashCode() : 0);
        result = 31 * result + (playerString != null ? playerString.hashCode() : 0);
        result = 31 * result + (spyString != null ? spyString.hashCode() : 0);
        result = 31 * result + (resString != null ? resString.hashCode() : 0);
        result = 31 * result + numPlayers;
        result = 31 * result + (rand != null ? rand.hashCode() : 0);
        result = 31 * result + fails;
        result = 31 * result + round;
        result = 31 * result + voteRound;
        result = 31 * result + leader;
        result = 31 * result + (team != null ? team.hashCode() : 0);
        result = 31 * result + (phase != null ? phase.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "State{" +
                "players=" + players +
                ", spies=" + spies +
                ", playerString='" + playerString + '\'' +
                ", spyString='" + spyString + '\'' +
                ", resString='" + resString + '\'' +
                ", numPlayers=" + numPlayers +
                ", rand=" + rand +
                ", fails=" + fails +
                ", round=" + round +
                ", voteRound=" + voteRound +
                ", leader=" + leader +
                ", team='" + team + '\'' +
                ", phase=" + phase +
                '}';
    }

    public enum Phase {
        NOT_STARTED,
        NOMINATING,
        VOTING,
        MISSION,
        COMPLETE
    }

}
