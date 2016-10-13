package agent;

import com.sun.org.apache.regexp.internal.RE;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Author: Sam Marsh
 * Date: 11/10/2016
 */
public class GameState {

    private final int numberOfPlayers;
    private final int numberOfSpies;
    private final Player me;
    private final Map<Character, Player> players;
    private final Map<Character, Player> spies;
    private final Team team;

    private int round;
    private int failures;
    private Mission proposedMission;
    private Mission mission;

    public GameState(String _me, String _players, String _spies) {
        players = new HashMap<Character, Player>();
        spies = new HashMap<Character, Player>();
        numberOfPlayers = _players.length();
        numberOfSpies = _spies.length();
        team = _spies.contains("?") ? Team.RESISTANCE : Team.GOVERNMENT;
        round = 0;
        failures = 0;
        proposedMission = null;
        mission = null;

        if (team == Team.GOVERNMENT) System.exit(0);

        double suspicionSpy = (double) numberOfSpies() / numberOfPlayers();
        double suspicionResistance = (double) numberOfSpies() / (numberOfPlayers() - 1);

        me = new Player(this, _me.charAt(0), team == Team.RESISTANCE ? 0 : suspicionSpy);

        players.put(me.id(), me);
        for (char id : _players.toCharArray()) {
            Player player = new Player(this, id, team == Team.RESISTANCE ? suspicionResistance : suspicionSpy);
            if (player.id() != me.id()) {
                players.put(id, player);
            }
            if (_spies.contains(player.toString())) {
                spies.put(id, player);
            }
        }
    }

    public Player me() {
        return me;
    }

    public Collection<Player> players() {
        return players.values();
    }

    public Collection<Player> spies() {
        return spies.values();
    }

    public int numberOfSpies() {
        return numberOfSpies;
    }

    public int numberOfPlayers() {
        return numberOfPlayers;
    }

    public int round() {
        return round;
    }

    public void missionNumber(int _missionNumber) {
        round = _missionNumber;
    }

    public void failures(int _failures) {
        failures = _failures;
    }

    public int resistancePoints() {
        return round - failures - 1;
    }

    public int spyPoints() {
        return failures;
    }

    public Team team() {
        return team;
    }

    public Player lookup(char id) {
        return players.get(id);
    }

    public void proposedMission(Mission _mission) {
        proposedMission = _mission;
    }

    public Mission proposedMission() {
        return proposedMission;
    }

    public void mission(Mission _mission) {
        mission = _mission;
    }

    public Mission mission() {
        return mission;
    }

    @Override
    public String toString() {
        return "GameState{" +
                "numberOfPlayers=" + numberOfPlayers +
                ", players=" + players +
                ", numberOfSpies=" + numberOfSpies +
                ", me=" + me +
                ", team=" + team +
                ", round=" + round +
                ", resistancePoints=" + resistancePoints() +
                ", spyPoints=" + spyPoints() +
                ", mission=" + mission() +
                '}';
    }

}
