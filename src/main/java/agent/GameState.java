package agent;

import java.util.Arrays;

/**
 * Author: Sam Marsh
 * Date: 15/10/2016
 */
public class GameState {

    private final int numberOfPlayers;
    private final int numberOfSpies;
    private final char[] players;

    private int round;
    private int failures;
    private Mission proposedMission;
    private Mission mission;

    public GameState(String players, String spies) {
        this.players = players.toCharArray();
        this.numberOfPlayers = players.length();
        this.numberOfSpies = spies.length();
        this.round = 0;
        this.failures = 0;
        this.proposedMission = null;
        this.mission = null;
    }

    public char[] players() {
        return players;
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

    public boolean gameOver() {
        return round() == 6;
    }

    @Override
    public String toString() {
        return String.format(
                "GameState{numberOfPlayers=%d, numberOfSpies=%d, players=%s, round=%d, failures=%d}",
                numberOfPlayers, numberOfSpies, Arrays.toString(players), round, failures);
    }

}
