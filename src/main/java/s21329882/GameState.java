package s21329882;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class GameState {

    private final int numberOfPlayers;
    private final int numberOfSpies;
    private final char[] players;

    private int round;
    private int failures;
    private Mission proposedMission;
    private Mission mission;

    GameState(String players, String spies) {
        this.players = players.toCharArray();
        this.numberOfPlayers = players.length();
        this.numberOfSpies = spies.length();
        this.round = 0;
        this.failures = 0;
        this.proposedMission = null;
        this.mission = null;
    }

    boolean gameOver() {
        return round == 6;
    }

    public char[] players() {
        return players;
    }

    int numberOfSpies() {
        return numberOfSpies;
    }

    int numberOfPlayers() {
        return numberOfPlayers;
    }

    int round() {
        return round;
    }

    void missionNumber(int _missionNumber) {
        round = _missionNumber;
    }

    public void failures(int _failures) {
        failures = _failures;
    }

    int resistancePoints() {
        return round - failures - 1;
    }

    int spyPoints() {
        return failures;
    }

    void proposedMission(Mission _mission) {
        proposedMission = _mission;
    }

    Mission proposedMission() {
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
        return String.format(
                "GameState{numberOfPlayers=%d, numberOfSpies=%d, players=%s, round=%d, failures=%d}",
                numberOfPlayers, numberOfSpies, Arrays.toString(players), round, failures);
    }

    enum Team {

        RESISTANCE,
        GOVERNMENT

    }

    static class Mission {

        private final char leader;
        private final Set<Character> team;

        private int traitors;
        private String yays;

        Mission(String _leader, String _team) {
            leader = _leader.charAt(0);
            team = new HashSet<Character>();
            for (char id : _team.toCharArray()) {
                team.add(id);
            }
            traitors = -1;
        }

        boolean done() {
            return traitors != -1;
        }

        void undo() {
            this.traitors = -1;
        }

        void done(int traitors) {
            this.traitors = traitors;
        }

        void voted(String yays) {
            this.yays = yays;
        }

        public String yays() {
            return yays;
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
