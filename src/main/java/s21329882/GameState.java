package s21329882;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Author: Sam Marsh
 * Date: 15/10/2016
 */
public class GameState {

    private final int numberOfPlayers;
    private final int numberOfSpies;
    private final char[] players;

    private int missionProposalTries;
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
        this.missionProposalTries = 0;
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
        missionProposalTries = 0;
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
        ++missionProposalTries;
    }

    public int proposalTries() {
        return missionProposalTries;
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

    public enum Team {

        RESISTANCE,
        GOVERNMENT

    }

    public static class Mission {

        private final char leader;
        private final Set<Character> team;

        private int traitors;

        public Mission(String _leader, String _team) {
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

}
