package s21324325;

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

    private Map<Character, Perspective> map;

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
     * Updates the number of traitors on the mission.
     *
     * @param traitors the number of traitors
     */
    public void traitors(int traitors) {
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
    public void votes(int votes) {
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
    public int round() {
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

    public void update(String mission, int traitors) {
        char[] array = mission.toCharArray();
        for (Perspective perspective : map.values()) {
            perspective.update(array, traitors);
        }
    }

    public Collection<Perspective> perspectives() {
        return map.values();
    }

    @Override
    public Map<MCTS.Transition, Double> weightedTransitions() {
        Map<MCTS.Transition, Double> transitions = new HashMap<MCTS.Transition, Double>();

        switch (phase) {
            case NOMINATION: {
                if (me == players.charAt(currentLeader) || contains(spies, players.charAt(currentLeader))) {
                    for (String s : combinations(players, MISSION_NUMBERS[numberOfPlayers() - 5][round - 1])) {
                        transitions.put(new ResistanceTransition.Nomination(s), 1.0);
                    }
                } else {
                    Perspective perspective = map.get(players.charAt(currentLeader));
                    List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>();
                    for (String s : combinations(players, MISSION_NUMBERS[numberOfPlayers() - 5][round - 1])) {
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
                    //weight transitions such that resistance members are less likely to nominate teams which they think
                    // are likely to contain spies TODO improve this/experiment with this weighting
                    for (int i = 0; i < list.size(); ++i) {
                        transitions.put(new ResistanceTransition.Nomination(list.get(i).getKey()), (double) (i + 1) / list.size());
                    }
                }
                return transitions;
            }
            case MISSION: {
                if (contains(mission, players.charAt(currentPlayer)) && contains(spies, players.charAt(currentPlayer))) {
                    transitions.put(new ResistanceTransition.Sabotage(true), 1.0);
                }
                transitions.put(new ResistanceTransition.Sabotage(false), 1.0);
                return transitions;
            }
            case VOTING: {
                if (me == players.charAt(currentPlayer) || contains(spies, players.charAt(currentPlayer))) {
                    transitions.put(new ResistanceTransition.Vote(true), 1.0);
                    if (currentLeader != currentPlayer)
                        transitions.put(new ResistanceTransition.Vote(false), 1.0);
                } else {
                    Perspective perspective = map.get(players.charAt(currentPlayer));
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

    @Override
    public List<MCTS.Transition> transitions() {
        List<MCTS.Transition> set = new ArrayList<MCTS.Transition>();

        switch (phase) {
            case NOMINATION: {
                for (String s : combinations(players, MISSION_NUMBERS[numberOfPlayers() - 5][round - 1])) {
                    set.add(new ResistanceTransition.Nomination(s));
                }
                return set;
            }
            case MISSION: {
                if (contains(mission, players.charAt(currentPlayer)) && contains(spies, players.charAt(currentPlayer))) {
                    set.add(new ResistanceTransition.Sabotage(true));
                }
                set.add(new ResistanceTransition.Sabotage(false));

                return set;
            }
            case VOTING: {
                set.add(new ResistanceTransition.Vote(true));
                set.add(new ResistanceTransition.Vote(false));
                return set;
            }
        }

        return set;
    }

    private int startPlayer;

    @Override
    public void transition(MCTS.Transition transition) {
        if (transition instanceof ResistanceTransition.Nomination) {
            mission = ((ResistanceTransition.Nomination) transition).selection();
            phase = Phase.VOTING;
            votes = 0;
            currentLeader = after(currentLeader);
            startPlayer = players.indexOf(me);
            currentPlayer = players.indexOf(me);
        } else if (transition instanceof ResistanceTransition.Vote) {
            votes += ((ResistanceTransition.Vote) transition).yes() ? 1 : 0;
            if (currentPlayer != before(startPlayer)) {
                currentPlayer = after(currentPlayer);
            } else {
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
            traitors += ((ResistanceTransition.Sabotage) transition).sabotage() ? 1 : 0;
            if (currentPlayer != before(startPlayer)) {
                currentPlayer = after(currentPlayer);
            } else {
                startPlayer = players.indexOf(me);
                currentPlayer = players.indexOf(me);
                if (traitors != 0 && (traitors != 1 || round != 4 || numberOfPlayers() < 7)) {
                    failures++;
                }
                try {
                    update(mission, traitors);
                } catch (AssertionError e) {
                    //TODO check if this error has disappeared completely???
                    System.out.println(this + " " + currentPlayer + " " + startPlayer);
                    throw new Error(e);
                }
                traitors = 0;
                phase = Phase.NOMINATION;
                nominationAttempt = 1;
                votes = 0;
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
        return phase == Phase.NOMINATION ? currentLeader : currentPlayer;
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

    public void startPlayer(int player) {
        this.startPlayer = player;
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
