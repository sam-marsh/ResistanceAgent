package s21329882;

import java.util.*;

/**
 * This represents the state of the game from the perspective of a spy.
 * That is, we have perfect information.
 */
public class GameState implements MCTS.State {

    /**
     * MISSION_NUMBERS[nplayers-5][round-1] is the number of players on a mission this round
     */
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
     * Holds the perspective of each resistance member in the game, to use as an opponent model: players
     * are less likely to choose teams which they think contain spies, etc.
     */
    private Map<Character, Perspective> map;

    /**
     * Used to keep track of game state (which players turn it is). When we reach player (startPlayer - 1), the
     * voting stage is over (as an example).
     */
    private int startPlayer;

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
        this.currentPlayer = players.indexOf(me);
        this.startPlayer = players.indexOf(me);
        this.map = new HashMap<Character, Perspective>(players.length() - spies.length());
        for (char id : players.toCharArray()) {
            if (!contains(spies, id)) {
                map.put(id, new Perspective(id, players.toCharArray(), spies.length()));
            }
        }
    }

    /**
     * @return the players in the game
     */
    public String players() {
        return players;
    }

    /**
     * @return my player identifier
     */
    public char me() {
        return me;
    }

    /**
     * Updates the number of traitors on the mission.
     *
     * @param traitors the number of traitors
     */
    public void traitors(int traitors) {
        this.traitors = traitors;
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
     * Updates the mission.
     *
     * @param mission the new proposed/executing mission
     */
    public void mission(String mission) {
        this.mission = mission;
    }

    /**
     * Shifts to the next leader.
     */
    private void nextLeader() {
        currentLeader = after(currentLeader);
    }

    /**
     * Sets the leader (nominator).
     *
     * @param leader an index into the array of players
     */
    public void currentLeader(int leader) {
        this.currentLeader = leader;
        this.currentPlayer = leader;
        this.startPlayer = leader;
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
     * Updates the number of failures.
     *
     * @param failures how many missions have failed
     */
    public void failures(int failures) {
        this.failures = failures;
    }

    /**
     * Updates the current round of the game.
     * @param round the new round
     */
    public void round(int round) {
        this.round = round;
    }

    /**
     * Updates the current player. Important to call this before search so that we are simulating from the correct
     * player's point of view.
     *
     * @param player the index in the player string of the current player
     */
    public void currentPlayer(int player) {
        this.currentPlayer = player;
        this.startPlayer = player;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MCTS.State copy() {
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
        state.startPlayer = startPlayer;
        state.map = new HashMap<Character, Perspective>(map.size());
        for (Map.Entry<Character, Perspective> entry : map.entrySet()) {
            state.map.put(entry.getKey(), new Perspective(entry.getValue()));
        }
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<MCTS.Transition, Double> weightedTransitions() {
        Map<MCTS.Transition, Double> transitions = new HashMap<MCTS.Transition, Double>();

        switch (phase) {
            case NOMINATION: {
                if (contains(spies, players.charAt(currentLeader))) {
                    //if i am the current player or is another spy, add each transition with equal probability
                    for (String s : combinations(players, MISSION_NUMBERS[numPlayers() - 5][round - 1])) {
                        if (contains(s, players.charAt(currentLeader))) {
                            if (contains(spies, players.charAt(currentLeader))) {
                                if (numSpies(s) <= numSabotagesRequiredForPoint()) {
                                    transitions.put(new ResistanceTransition.Nomination(s), 1.0);
                                }
                            } else {
                                transitions.put(new ResistanceTransition.Nomination(s), 1.0);
                            }
                        }
                    }
                } else {
                    //weight transitions such that resistance members are less likely to nominate teams which they think
                    // are likely to contain spies
                    Perspective perspective = map.get(players.charAt(currentLeader));
                    List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>();
                    for (String s : combinations(players, MISSION_NUMBERS[numPlayers() - 5][round - 1])) {
                        if (contains(s, players.charAt(currentLeader))) {
                            double suspicion = 0;
                            for (char c : s.toCharArray()) {
                                suspicion += perspective.lookup(c);
                            }
                            list.add(new AbstractMap.SimpleImmutableEntry<String, Double>(s, suspicion));
                        }
                    }
                    Collections.shuffle(list);
                    Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                        @Override
                        public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                            return (int) Math.signum(o2.getValue() - o1.getValue());
                        }
                    });
                    double last = list.get(0).getValue();
                    int index = 1;
                    for (int i = 0; i < list.size(); ++i) {
                        String nomination = list.get(i).getKey();
                        double suspicion = list.get(i).getValue();
                        if (suspicion != last) {
                            ++index;
                        }
                        transitions.put(new ResistanceTransition.Nomination(nomination), (double) index / list.size());
                    }
                }
                return transitions;
            }
            case MISSION: {
                //add sabotage and not sabotage with equal probability since opponent model only considers resistance members
                if (contains(mission, players.charAt(currentPlayer)) && contains(spies, players.charAt(currentPlayer))) {
                    transitions.put(new ResistanceTransition.Sabotage(true), 1.0);
                }
                transitions.put(new ResistanceTransition.Sabotage(false), 1.0);
                return transitions;
            }
            case VOTING: {
                if (currentPlayer == currentLeader) {
                    transitions.put(new ResistanceTransition.Vote(true), 1.0);
                    return transitions;
                }
                if (contains(spies, players.charAt(currentPlayer))) {
                    //me or another spy, so add each choice with equal weight
                    transitions.put(new ResistanceTransition.Vote(true), 1.0);
                    if (currentLeader != currentPlayer)
                        transitions.put(new ResistanceTransition.Vote(false), 1.0);
                } else {
                    Perspective perspective = map.get(players.charAt(currentPlayer));

                    //weight transitions such that resistance members are less likely to vote for teams which they
                    // think are likely to contain spies

                    double suspicion = 0;
                    for (char c : mission.toCharArray()) {
                        suspicion += perspective.lookup(c);
                    }

                    transitions.put(new ResistanceTransition.Vote(true), mission.length() - suspicion);
                    transitions.put(new ResistanceTransition.Vote(false), suspicion);
                }
                return transitions;
            }
        }

        throw new AssertionError();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MCTS.Transition> transitions() {
        List<MCTS.Transition> set = new ArrayList<MCTS.Transition>();

        switch (phase) {
            case NOMINATION: {
                //add every possible nomination
                for (String s : combinations(players, MISSION_NUMBERS[numPlayers() - 5][round - 1])) {
                    if (contains(s, players.charAt(currentLeader))) {
                        if (contains(spies, players.charAt(currentLeader))) {
                            if (numSpies(s) <= numSabotagesRequiredForPoint()) {
                                set.add(new ResistanceTransition.Nomination(s));
                            }
                        } else {
                            set.add(new ResistanceTransition.Nomination(s));
                        }
                    }
                }
                return set;
            }
            case MISSION: {
                //add true if a spy, otherwise can only vote false to sabotage
                if (contains(mission, players.charAt(currentPlayer)) && contains(spies, players.charAt(currentPlayer))) {
                    set.add(new ResistanceTransition.Sabotage(true));
                }
                set.add(new ResistanceTransition.Sabotage(false));

                return set;
            }
            case VOTING: {
                set.add(new ResistanceTransition.Vote(true));
                if (currentPlayer != currentLeader)
                    set.add(new ResistanceTransition.Vote(false));
                return set;
            }
        }

        return set;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void transition(MCTS.Transition transition) {
        if (transition instanceof ResistanceTransition.Nomination) {
            //nomination phase - choose a team and transition to the voting phase
            mission = ((ResistanceTransition.Nomination) transition).selection();
            phase = Phase.VOTING;
            votes = 0;
            startPlayer = players.indexOf(me);
            currentPlayer = players.indexOf(me);
        } else if (transition instanceof ResistanceTransition.Vote) {
            //add to the current vote
            votes += ((ResistanceTransition.Vote) transition).yes() ? 1 : 0;
            if (currentPlayer != before(startPlayer)) {
                //not the final player - continue voting
                currentPlayer = after(currentPlayer);
            } else {
                //voting done - now transition to the appropriate next phase
                startPlayer = players.indexOf(me);
                currentPlayer = players.indexOf(me);
                if (votes > players.length() / 2 || nominationAttempt == 5) {
                    phase = Phase.MISSION;
                    traitors = 0;
                    nominationAttempt = 1;
                } else {
                    nextLeader();
                    phase = Phase.NOMINATION;
                    nominationAttempt++;
                }
                votes = 0;
            }
        } else if (transition instanceof ResistanceTransition.Sabotage) {
            //mission phase - add to the number of traitors
            traitors += ((ResistanceTransition.Sabotage) transition).sabotage() ? 1 : 0;
            if (currentPlayer != before(startPlayer)) {
                //not the final player - continue
                currentPlayer = after(currentPlayer);
            } else {
                //mission done - move to the next phase
                startPlayer = players.indexOf(me);
                currentPlayer = players.indexOf(me);
                if (traitors != 0 && (traitors != 1 || round != 4 || numPlayers() < 7)) {
                    failures++;
                }
                //update perspectives
                update(mission, traitors);
                traitors = 0;
                phase = Phase.NOMINATION;
                nominationAttempt = 1;
                votes = 0;
                round++;
                nextLeader();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean complete() {
        return round == 6;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int currentPlayer() {
        return phase == Phase.NOMINATION ? currentLeader : currentPlayer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int numPlayers() {
        return players().length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] scores() {
        int[] scores = new int[numPlayers()];
        for (int i = 0; i < scores.length; ++i) {
            if (contains(spies, players.charAt(i))) {
                //player has won if three or more sabotages
                scores[i] = spyPoints() >= 3 ? 1 : 0;
            } else {
                //player has won if three or more successful missions
                scores[i] = resistancePoints() >= 3 ? 1 : 0;
            }
        }
        return scores;
    }

    /**
     * {@inheritDoc}
     */
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
     * @return the number of successful sabotages
     */
    private int resistancePoints() {
        return round - failures - 1;
    }

    /**
     * @return the number of missions that have been sabotaged
     */
    private int spyPoints() {
        return failures;
    }

    /**
     * Updates the perspective of each resistance member.
     *
     * @param mission the mission members
     * @param traitors the number of traitors
     */
    public void update(String mission, int traitors) {
        char[] array = mission.toCharArray();
        for (Perspective perspective : map.values()) {
            perspective.update(array, traitors);
        }
    }

    /**
     * Gives the set of all combinations of the given string having length n.
     *
     * @param s the string to find combinations in
     * @param n the size of each combination
     * @return a set containing all unique combinations
     */
    private Set<String> combinations(String s, int n) {
        Set<String> set = new HashSet<String>();
        combinations(s.toCharArray(), n, 0, new char[n], set);
        return set;
    }

    /**
     * Iterates over every combination of a string of a certain length and adds them to a set.
     *
     * @param array the string to find combinations for, as a character array
     * @param len set to 0
     * @param start set to 0
     * @param result set to a char array of the size of combination wanted
     * @param set an empty set
     */
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

    /**
     * @param i an integer representing a player index
     * @return the player before the given player
     */
    private int before(int i) {
        return (i - 1 + players().length()) % players().length();
    }

    /**
     * @param i an integer representing a player index
     * @return the player after the given player
     */
    private int after(int i) {
        return (i + 1) % players.length();
    }

    /**
     * Checks if a string contains a character.
     *
     * @param s the string
     * @param c the character to check
     * @return true if the string contains the character
     */
    private boolean contains(String s, char c) {
        return s.indexOf(c) != -1;
    }

    /**
     * @param mission the mission to consider
     * @return the number of spies on that mission
     */
    private int numSpies(String mission) {
        int count = 0;
        for (char c : mission.toCharArray()) {
            if (contains(spies, c))
                ++count;
        }
        return count;
    }

    /**
     * @return how many spies are needed on the team to be able to win a point
     */
    private int numSabotagesRequiredForPoint() {
        return round == 4 && numPlayers() >= 7 ? 2 : 1;
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
