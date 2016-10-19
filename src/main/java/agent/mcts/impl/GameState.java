package agent.mcts.impl;

import agent.mcts.State;
import agent.mcts.Transition;
import agent.mcts.impl.transition.SabotageTransition;
import agent.mcts.impl.transition.NominationTransition;
import agent.mcts.impl.transition.VoteTransition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This represents the state of the game from the perspective of a spy.
 * That is, we have perfect information.
 */
public class GameState implements State {

    private static final int[][] MISSION_NUMBERS = {
            { 2, 3, 2, 3, 3 },
            { 2, 3, 4, 3, 4 },
            { 2, 3, 3, 4, 4 },
            { 3, 4, 4, 5, 5 },
            { 3, 4, 4, 5, 5 },
            { 3, 4, 4, 5, 5 }
    };

    /**
     * All players in the game. The order is important because the order of leaders
     * who select the team is dependent on this ordering.
     */
    private final String players;

    /**
     * The spies in the game.
     */
    private final String spies;

    /**
     * Which character I am.
     */
    private final char me;

    /**
     * Holds the current game phase.
     */
    private Phase phase;

    /**
     * The current player is the one who 'chooses' a transition next. The integer represents the player's
     * index in the {@link GameState#players} string. This applies in the voting and mission stages.
     */
    private int currentPlayer;

    /**
     * As above, this holds an index into the player string. This represents the current leader, i.e.
     * who is proposing the current mission.
     */
    private int currentLeader;

    /**
     * The current round of the game, ranging from 1..6 (where 6 means the game has ended).
     */
    private int round;

    /**
     * The number of missions that have been sabotaged. The spies win if this is three or more.
     */
    private int failures;

    /**
     * The attempt number for mission/team nomination. If this reaches five, the team is automatically
     * passed.
     */
    private int nominationAttempt;

    /**
     * The number of votes for the current proposed mission.
     */
    private int votes;

    /**
     * The current mission. Used for both the proposed mission and the actual mission once it is chosen.
     */
    private String mission;

    /**
     * The number of spies that sabotaged the mission.
     */
    private int traitors;

    /**
     * Creates a new game state with given resistance players and government spies.
     *
     * @param players all players
     * @param spies the spies
     * @param me my character identifier
     */
    public GameState(String players, String spies, char me) {
        this.players = players;
        this.spies = spies;
        this.me = me;
        this.nominationAttempt = 1;
    }

    /**
     * Updates the number of traitors on the mission.
     *
     * @param traitors the number of traitors
     */
    private void traitors(int traitors) {
        this.traitors = traitors;
    }

    /**
     * @return the number of traitors on a mission
     */
    private int traitors() {
        return traitors;
    }

    /**
     * Updates the game phase.
     *
     * @param phase the new phase
     */
    public void phase(Phase phase) {
        this.phase = phase;
    }

    /**
     * @return the current phase of the game
     */
    private Phase phase() {
        return phase;
    }

    /**
     * Updates the mission.
     *
     * @param mission the new proposed/executing mission
     */
    public void mission(String mission) {
        this.mission = mission;
    }

    /**
     * @return the current mission, either being proposed or executed
     */
    private String mission() {
        return mission;
    }

    /**
     * Updates the number of votes for the proposed mission.
     *
     * @param votes the number of votes
     */
    private void votes(int votes) {
        this.votes = votes;
    }

    /**
     * @return the number of votes for the mission currently being proposed
     */
    private int votes() {
        return votes;
    }

    /**
     * Shifts to the next leader.
     */
    private void nextLeader() {
        currentLeader = (currentLeader + 1) % numberOfPlayers();
    }

    /**
     * @return the current mission leader
     */
    public int currentLeader() {
        return currentLeader;
    }

    public void currentLeader(int leader) {
        this.currentLeader = leader;
    }

    /**
     * Updates the nomination attempt. Shouldn't be set to anything above five.
     *
     * @param attempt the attempt number
     */
    public void nominationAttempt(int attempt) {
        this.nominationAttempt = attempt;
    }

    /**
     * @return the attempt number for team nomination
     */
    public int nominationAttempt() {
        return nominationAttempt;
    }

    /**
     * @return the number of successful sabotages
     */
    private int resistancePoints() {
        return round - failures - 1; //TODO check for off-by-one error
    }

    /**
     * @return the number of missions that have been sabotaged
     */
    private int spyPoints() {
        return failures;
    }

    /**
     * Updates the number of failures.
     *
     * @param failures how many missions have failed
     */
    public void failures(int failures) {
        this.failures = failures;
    }

    /**
     * @return the current round of the game, from 1..6 where 6 means the game has ended
     */
    private int round() {
        return round;
    }

    /**
     * Updates the current round of the game.
     * @param round the new round
     */
    public void round(int round) {
        this.round = round;
    }

    /**
     * Increments the current player index by one, i.e. transitions to the next player's turn.
     */
    private void nextPlayer() {
        currentPlayer = (currentPlayer + 1) % numberOfPlayers();
    }

    /**
     * @return the number of players
     */
    private int numberOfPlayers() {
        return players.length();
    }

    /**
     * @return the number of spies
     */
    private int numberOfSpies() {
        return spies.length();
    }

    @Override
    public State copy() {
        GameState state = new GameState(players, spies, me);
        state.phase = phase;
        state.currentPlayer = currentPlayer;
        state.currentLeader = currentLeader;
        state.round = round;
        state.failures = failures;
        state.nominationAttempt = nominationAttempt;
        state.votes = votes;
        state.mission = mission;
        state.traitors = traitors;
        return state;
    }

    @Override
    public List<Transition> transitions() {
        List<Transition> set = new ArrayList<Transition>();

        switch (phase) {
            case NOMINATION: {
                for (String s : combinations(players, MISSION_NUMBERS[numberOfPlayers() - 5][round - 1])) {
                    set.add(new NominationTransition(s));
                }
                return set;
            }
            case MISSION: {
                if (contains(mission, players.charAt(currentPlayer))) {
                    set.add(new SabotageTransition(true));
                }
                set.add(new SabotageTransition(false));
                return set;
            }
            case VOTING:
                set.add(new VoteTransition(true));
                set.add(new VoteTransition(false));
                return set;
        }

        return set;
    }

    private int startPlayer;

    @Override
    public void transition(Transition transition) {
        if (transition instanceof NominationTransition) {
            mission = ((NominationTransition) transition).selection();
            phase = Phase.VOTING;
            currentPlayer = players.indexOf(me);
            startPlayer = players.indexOf(me);
            votes = 0;
        } else if (transition instanceof VoteTransition) {
            votes += ((VoteTransition) transition).yes() ? 1 : 0;
            if (currentPlayer != before(startPlayer)) {
                currentPlayer = after(currentPlayer);
            } else {
                if (votes >= Math.ceil((double) players.length() / 2) || nominationAttempt == 5) {
                    phase = Phase.MISSION;
                    traitors = 0;
                } else {
                    nextLeader();
                    phase = Phase.NOMINATION;
                }
                startPlayer = players.indexOf(me);
                currentPlayer = players.indexOf(me);
                nominationAttempt++;
                votes = 0;
            }
        } else if (transition instanceof SabotageTransition) {
            traitors += ((SabotageTransition) transition).sabotage() ? 1 : 0;
            if (currentPlayer != before(startPlayer)) {
                currentPlayer = after(currentPlayer);
            } else {
                if (traitors != 0 && (traitors != 1 || round != 4 || numberOfPlayers() < 7)) {
                    failures++;
                }
                phase = Phase.NOMINATION;
                nextLeader();
                startPlayer = players.indexOf(me);
                currentPlayer = players.indexOf(me);
                nominationAttempt = 1;
                round++;
            }
        }
    }

    @Override
    public boolean complete() {
        return round == 6;
    }

    @Override
    public int currentPlayer() {
        return currentPlayer;
    }

    public void currentPlayer(int player) {
        this.currentPlayer = player;
        this.startPlayer = player;
    }

    @Override
    public int numPlayers() {
        return players().length();
    }

    @Override
    public int[] scores() {
        int[] scores = new int[numberOfPlayers()];
        for (int i = 0; i < scores.length; ++i) {
            if (spies.indexOf(players.charAt(i)) != -1) {
                scores[i] = spyPoints() >= 3 ? 1 : 0;
            } else {
                scores[i] = resistancePoints() >= 3 ? 1 : 0;
            }
        }
        return scores;
    }

    private void combinations(char[] array, int len, int start, char[] result, Set<String> set) {
        if (len == 0) {
            set.add(new String(result));
            return;
        }
        for (int i = start; i <= array.length - len; ++i) {
            result[result.length - len] = array[i];
            combinations(array, len - 1, i + 1, result, set);
        }
    }

    private int before(int i) {
        return (i - 1 + players().length()) % players().length();
    }

    private int after(int i) {
        return (i + 1) % players.length();
    }

    private Set<String> combinations(String s, int n) {
        Set<String> set = new HashSet<String>();
        combinations(s.toCharArray(), n, 0, new char[n], set);
        return set;
    }

    private int numSpiesOnMissionNotIncludingMe() {
        int i = 0;
        for (char c : mission.toCharArray()) {
            if (contains(spies, c) && c != me) {
                ++i;
            }
        }
        return i;
    }

    private boolean contains(String s, char c) {
        return s.indexOf(c) != -1;
    }

    public String players() {
        return players;
    }

    public char me() {
        return me;
    }

    @Override
    public String toString() {
        return String.format(
                "GameState{players='%s', spies='%s', me=%s, phase=%s, currentPlayer=%d, currentLeader=%d, round=%d, " +
                        "failures=%d, nominationAttempt=%d, votes=%d, mission='%s', traitors=%d}",
                players, spies, me, phase, currentPlayer, currentLeader, round, failures,
                nominationAttempt, votes, mission, traitors
        );
    }

    /**
     * Holds the possible states of the game.
     */
    public enum Phase {

        /**
         * The nomination phase, where a leader picks a team of a given size.
         */
        NOMINATION,

        /**
         * The voting stage - voting yes or no for a particular team.
         */
        VOTING,

        /**
         * The mission stage, where the spies choose whether to sabotage.
         */
        MISSION

    }


}
