package s21324325;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds some data relating to the current game/round etc.
 */
public class GameState {

    //total number of players in the game, from 5-10
    private final int numberOfPlayers;

    //total number of spies in the game, from 2-4
    private final int numberOfSpies;

    //each character's identifier
    private final char[] players;

    //the current round
    private int round;

    //the number of missions that have failed
    private int failures;

    //the mission currently being voted on
    private Mission proposedMission;

    //the mission currently being 'executed'
    private Mission mission;

    /**
     * Initialises a new game state to carry information.
     *
     * @param players the player identifiers
     * @param spies the spy identifiers
     */
    public GameState(String players, String spies) {
        this.players = players.toCharArray();
        this.numberOfPlayers = players.length();
        this.numberOfSpies = spies.length();
        this.round = 0;
        this.failures = 0;
        this.proposedMission = null;
        this.mission = null;
    }

    /**
     * @return true if and only if the game is finished
     */
    public boolean gameOver() {
        return round == 6;
    }

    /**
     * @return the player identifiers of all players in the game
     */
    public char[] players() {
        return players;
    }

    /**
     * @return the total number of spies in the game
     */
    public int numberOfSpies() {
        return numberOfSpies;
    }

    /**
     * @return the total number of players in the game
     */
    public int numberOfPlayers() {
        return numberOfPlayers;
    }

    /**
     * @return the current round from 1-5, or 6 if the game is over
     */
    public int round() {
        return round;
    }

    /**
     * Sets the current mission.
     *
     * @param round the round from 1-6 (6 if game over)
     */
    public void round(int round) {
        this.round = round;
    }

    /**
     * Sets the number of failed missions.
     *
     * @param failures the number of failed missions
     */
    public void failures(int failures) {
        this.failures = failures;
    }

    /**
     * @return the number of points for the resistance side
     */
    public int resistancePoints() {
        return round - failures - 1;
    }

    /**
     * @return the number of points for the spy side
     */
    public int spyPoints() {
        return failures;
    }

    /**
     * Sets the mission currently being voted on.
     *
     * @param mission the mission
     */
    public void proposedMission(Mission mission) {
        proposedMission = mission;
    }

    /**
     * @return the mission currently being voted on
     */
    public Mission proposedMission() {
        return proposedMission;
    }

    /**
     * Sets the mission currently being 'executed'.
     *
     * @param mission the mission in progress
     */
    public void mission(Mission mission) {
        this.mission = mission;
    }

    /**
     * @return the mission currently in progress
     */
    public Mission mission() {
        return mission;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format(
                "GameState{numberOfPlayers=%d, numberOfSpies=%d, players=%s, round=%d, failures=%d}",
                numberOfPlayers, numberOfSpies, Arrays.toString(players), round, failures);
    }

    /**
     * Represents a mission round in the game.
     */
    public static class Mission {

        //the leader of the mission - who nominated it
        private final char leader;

        //every player on the team (may or may not include the leader)
        private final Set<Character> team;

        //the number of sabotages
        private int traitors;

        //who voted for this mission
        private String yays;

        /**
         * Creates a new mission with the given leader and team.
         *
         * @param leader the mission leader
         * @param team the players on the team
         */
        public Mission(String leader, String team) {
            this.leader = leader.charAt(0);
            this.team = new HashSet<Character>();
            for (char id : team.toCharArray()) {
                this.team.add(id);
            }
            traitors = -1;
        }

        /**
         * @return whether this mission has been carried out yet
         */
        public boolean done() {
            return traitors != -1;
        }

        /**
         * Resets the mission so that it has 'not yet been executed' again. Used for simulation.
         */
        public void undo() {
            this.traitors = -1;
        }

        /**
         * Marks the mission as completed.
         *
         * @param traitors the number of spies that chose to sabotage the mission
         */
        public void done(int traitors) {
            this.traitors = traitors;
        }

        /**
         * Sets the players who have voted yes for the mission.
         *
         * @param yays the player identifiers of the players who voted yes
         */
        public void voted(String yays) {
            this.yays = yays;
        }

        /**
         * @return who voted yes for the mission to go ahead
         */
        public String yays() {
            return yays;
        }

        /**
         * @return the number of spies that sabotaged the mission
         */
        public int traitors() {
            return traitors;
        }

        /**
         * @return the identifier of the player who nominated this mission
         */
        public char leader() {
            return leader;
        }

        /**
         * @return all players on the team
         */
        public Set<Character> team() {
            return team;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("Mission{leader=%s, team=%s, done=%s, traitors=%d}", leader, team, done(), traitors);
        }

    }

}
