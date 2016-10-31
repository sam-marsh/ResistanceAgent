package s21324325;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

/**
 * Represents the perspective of a resistance player. Holds suspicion values for each player,
 * which are updated after game events (e.g. voting, missions etc.). Primarily uses Bayesian
 * inference.
 */
public class ResistancePerspective {

    //general game data
    private final GameState state;

    //which player i am
    private final Player me;

    //all game players
    private final Map<Character, Player> players;

    //all players not including me
    private final List<Player> others;

    /**
     * Creates a new perspective of the game from the point of view of a resistance player
     *
     * @param _state game data
     * @param _me my identifier
     * @param _players all players
     */
    public ResistancePerspective(GameState _state, String _me, String _players) {
        state = _state;
        players = new HashMap<Character, Player>();
        others = new ArrayList<Player>(_players.length() - 1);

        //suspicion values equally distributed
        double initialSuspicion = (double) _state.numberOfSpies() / (_state.numberOfPlayers() - 1);

        me = new Player(_me.charAt(0), 0);

        players.put(me.id(), me);
        for (char id : _players.toCharArray()) {
            if (id != me.id()) {
                Player player = new Player(id, initialSuspicion);
                players.put(id, player);
                others.add(player);
            }
        }
    }

    /**
     * @return all game players not including me
     */
    public List<Player> others() {
        return others;
    }

    /**
     * @return my character
     */
    public Player me() {
        return me;
    }

    /**
     * @return all game players
     */
    public Collection<Player> players() {
        return players.values();
    }

    /**
     * @param id the player identifier
     * @return the player object corresponding to the given identifier
     */
    public Player lookup(char id) {
        return players.get(id);
    }

    /**
     * Updates the suspicion for each player based on the current round evidence - i.e. number of sabotages.
     */
    public void updateSuspicion() {
        //remove line below - still extremely useful to update probabilities when mission succeeds, since
        // if no sabotages occur it is more likely that there were no spies on the team
        //if (traitors == 0) return;

        //need a temporary map to store new probabilities, since need all spy probabilities to remain constant while
        // computing the new ones
        Map<Player, Double> updated = new HashMap<Player, Double>(players().size());

        //need to create new list to hold the collection of players since need to access by index - bit annoying
        List<Player> players = new LinkedList<Player>(players());

        //P(B) - probability of cards being dealt - is constant for all players so can compute once
        //edit - don't need to spend resources on calculating this, can just normalise each probability at the end
        //double pb = computeProbabilityOfMissionSabotages(players);

        for (Player p : players()) {
            //P(A) - original probability of being a spy
            double pa = p.bayesSuspicion();

            //P(B|A) - probability of cards being dealt given that the player is a spy - just temporarily set
            // spy probability of player to 1
            p.bayesSuspicion(1.0);
            double pba = computeProbabilityOfMissionSabotages(players);
            p.bayesSuspicion(pa);

            //add new probability to map
            updated.put(p, bayes(pa, 1.0, pba));
        }

        double total = 0.0;
        for (Map.Entry<Player, Double> entry : updated.entrySet()) {
            total += entry.getValue();
        }

        //computation is done - update probabilities from map
        for (Map.Entry<Player, Double> entry : updated.entrySet()) {
            double newValue = entry.getValue() * state.numberOfSpies() / total;
            entry.getKey().bayesSuspicion(newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ResistancePerspective{me=%s21329882, players=%s21329882}", me, players);
    }


    /**
     * Computes the update probability given evidence using Bayes rule.
     *
     * @param pa P(A): the original probability, in this case the probability that the player is a spy
     * @param pb P(B): the probability of the event providing new evidence to have occurred, in this case the
     *           probability of the number of sabotages occurring
     * @param pba P(B|A): the probability of event B, given event A - in this case the probability of the number of
     *            sabotages occurring, given that the player is a spy
     * @return the updated probability of event A given evidence B - that is, P(A|B)
     */
    private double bayes(double pa, double pb, double pba) {
        if (pa == 0 || pba == 0) return 0;
        return pa * pba / pb;
    }


    /**
     * This method is for computing P(B) and P(B|A): it calculates the probability of the number of sabotages that
     * happened on this mission. It does this by iterating over all combinations of spies on the team, and within that
     * team, all combinations of each spy choosing to sabotage or not. This sounds computationally expensive but is
     * feasible for this game: with 10 players there are 4 spies, so there are (10 choose 4) possible combinations
     * of spies and up to 2^4 combinations for which spies choose to sabotage/not sabotage a mission: resulting in a
     * total of 3360 possibilities as an absolute maximum.
     *
     * @param players the list of players in the game
     * @return the probability of the mission resulting in the number of sabotages that occurred
     */
    private double computeProbabilityOfMissionSabotages(List<Player> players) {
        return computeProbabilityOfMissionSabotages(players, 0, 0, new boolean[players.size()]);
    }

    /**
     * The recursive method for {@link #computeProbabilityOfMissionSabotages(List)}.
     *
     * @param players the list of players
     * @param start recursive parameter to help iterate over combinations - pass 0
     * @param curr recursive parameter to help iterate over combinations - pass 0
     * @param spy recursive parameter to help iterate over combinations - pass a boolean array filled with the
     *             value {@code false} of size {@link GameState#numberOfPlayers()}
     * @return the probability of the mission resulting in the number of sabotages that occurred
     */
    private double computeProbabilityOfMissionSabotages(List<Player> players, int start, int curr, boolean[] spy) {
        //base case - have reached the correct combination size
        if (curr == state.numberOfSpies()) {
            //create list of spies and then iterate over all possibilities for which spies did/didn't sabotage
            List<Player> spies = new LinkedList<Player>();
            for (int i = 0; i < players.size(); ++i) if (spy[i]) spies.add(players.get(i));
            return sumSabotageCombinations(spies, 0, 0, new boolean[spies.size()]);
        }

        //base case - ensure not over the array size
        if (start == players.size()) return 0;

        //recurse - set the player in the first index to be a spy
        spy[start] = true;
        double tmp = computeProbabilityOfMissionSabotages(players, start + 1, curr + 1, spy);

        //recurse - set the player in the first index to be a true resistance member
        spy[start] = false;
        return tmp + computeProbabilityOfMissionSabotages(players, start + 1, curr, spy);
    }

    /**
     * If a spy is in a team, it has some probability to sabotage the mission. This method iterates over a given
     * spy combination and considers all combinations of which spies sabotaged/didn't sabotage. It sums the probabilities
     * and returns the total probability for the given number of sabotages to have occurred in a mission, assuming a certain
     * spy combination.
     *
     * @param spies the assumed list of spies (of size {@link GameState#numberOfSpies()})
     * @param start recursive parameter to help iterate over combinations - pass 0
     * @param curr recursive parameter to help iterate over combinations - pass 0
     * @param sabotaged recursive parameter to help iterate over combinations - pass a boolean array filled with the
     *                     value {@code false} of size {@link GameState#numberOfSpies()}
     * @return the total probability for a given set of spies to have sabotaged the number of times that the mission
     * was sabotaged
     */
    private double sumSabotageCombinations(List<Player> spies, int start, int curr, boolean sabotaged[]) {
        //base case - have reached the correct combination size
        if (curr == state.mission().traitors()) {

            //ensure that the number of sabotages in this combination caused by spies IN THE TEAM corresponds to the
            // number of sabotages that actually occurred
            int numSabotaged = 0;
            for (int i = 0; i < sabotaged.length; ++i) {
                Player p = spies.get(i);
                if (state.mission().team().contains(p.id()) && sabotaged[i])
                    numSabotaged++;
            }
            if (numSabotaged != state.mission().traitors()) return 0;

            List<Player> spiesOnMission = new LinkedList<Player>();
            for (Player p : spies) if (state.mission().team().contains(p.id())) spiesOnMission.add(p);

            double total = 1.0;
            for (int i = 0; i < sabotaged.length; ++i) {
                Player p = spies.get(i);
                if (state.mission().team().contains(p.id())) {
                    if (sabotaged[i]) {
                        //spy on mission and spy sabotaged
                        total *= p.bayesSuspicion() * p.likelihoodToBetray(spiesOnMission);
                    } else {
                        //spy on mission but didn't sabotage
                        total *= p.bayesSuspicion() * (1 - p.likelihoodToBetray(spiesOnMission));
                    }
                } else {
                    //spy not on mission
                    total *= p.bayesSuspicion();
                }
            }

            for (Player p : players()) {
                if (!spies.contains(p)) {
                    //not a spy
                    total *= (1 - p.bayesSuspicion());
                }
            }

            return total;
        }

        //base case - ensure not over the array size
        if (start == sabotaged.length) return 0;

        //recurse - set the spy in the start index to have sabotaged
        sabotaged[start] = true;
        double tmp = sumSabotageCombinations(spies, start + 1, curr + 1, sabotaged);

        //recurse - set the spy in the start index not to have sabotaged
        sabotaged[start] = false;
        return tmp + sumSabotageCombinations(spies, start + 1, curr, sabotaged);
    }

    public class Player {

        //how much influence this behaviour should have on the spyness - how much this player assisted spies
        public static final double HELPED_SPY_WEIGHT = 0.25;

        //how much influence this behaviour should have on the spyness - how much this player acted like a spy
        public static final double BEHAVED_LIKE_SPY_WEIGHT = 0.5;

        //how much influence this behaviour should have on the spyness - how much this player acted like a
        // resistance member
        public static final double BEHAVED_LIKE_RESISTANCE_WEIGHT = 0.1;

        //the weighting to assign when considering how friendly the members of each possible spy combination
        // have been to each other
        public static final double FRIENDSHIP_WEIGHT = 0.5;

        //this player's21329882 identifier
        private final char id;

        //friendship values for every other player
        private final Map<Player, Double> friends;

        //how much this player has assisted the spy team
        private final Argument helpedSpies;

        //how much this player has been acting like a spy
        private final Argument behavedLikeSpy;

        //how much this player has been acting like a true resistance member
        private final Argument behavedLikeResistance;

        //the spy probability calculated via Bayesian inference
        private double bayesSuspicion;

        /**
         * Creates a new player, which we are suspicious of.
         *
         * @param id the player identifier
         * @param initialSuspicion how much we suspect this player of being a spy
         */
        public Player(char id, double initialSuspicion) {
            this.id = id;
            this.friends = new HashMap<Player, Double>();
            this.helpedSpies = new Argument((double) state.numberOfSpies() / (state.numberOfPlayers() - 1), 1);
            this.behavedLikeSpy = new Argument(0, 0);
            this.behavedLikeResistance = new Argument(0, 0);
            this.bayesSuspicion = initialSuspicion;
        }

        /**
         * @return this player's21329882 identifier
         */
        public char id() {
            return id;
        }

        /**
         * Adds some evidence that this player is friends with another player.
         *
         * @param player the player which this player was friendly to
         * @param value how much weight (usually 1)
         * @param n how many samples (usually 1)
         */
        public void friendship(Player player, double value, int n) {
            Double current = friendship(player);
            friends.put(player, 1 - (1 - current) * (1 - value / n));
        }

        /**
         * @param player the other player to consider
         * @return a probability that this player is friends with the other player
         */
        public Double friendship(Player player) {
            Double friendship = friends.get(player);
            if (friendship == null) {
                friendship = (double) state.numberOfSpies() / (state.numberOfPlayers() - 1);
                friends.put(player, friendship);
            }
            return friendship;
        }

        /**
         * @return a variable holding samples for whether this player seems to be helping spies or not
         */
        public Argument helpedSpy() {
            return helpedSpies;
        }

        /**
         * @return a variable holding samples for whether this player is acting like a spy
         */
        public Argument behavedLikeSpy() {
            return behavedLikeSpy;
        }

        /**
         * @return a variable holding samples for whether this player is acting like a true resistance member
         */
        public Argument behavedLikeResistance() {
            return behavedLikeResistance;
        }

        /**
         * @return the probability that this player is a spy, using Bayesian inference
         */
        public double bayesSuspicion() {
            return bayesSuspicion;
        }

        /**
         * @param _bayesSuspicion sets the new probability that this player is a spy
         */
        public void bayesSuspicion(double _bayesSuspicion) {
            //avoid rounding error taking it above 1 or below 0
            bayesSuspicion = Math.min(Math.max(_bayesSuspicion, 0), 1);
        }

        /**
         * Gives the likelihood of this player to betray a mission, given that it is on the team and assuming it is a spy.
         *
         * @param spiesOnMission the spies on the mission team (not including those left out of the team)
         * @return the likelihood that the player will betray the mission
         */
        public double likelihoodToBetray(Collection<Player> spiesOnMission) {
            if (spiesOnMission.size() == 1) return 0.95;
            return 1.0 / spiesOnMission.size();
        }

        /**
         * @return a heuristic-style estimate of how spy-ish this player is
         */
        public double spyness() {
            //if we know for certain through Bayesian inference, use that
            if (bayesSuspicion == 0) return 0;
            if (bayesSuspicion == 1) return 1;

            //otherwise, weight using other suspicion tracking variables
            double value = bayesSuspicion();
            value *= ((1 - HELPED_SPY_WEIGHT) + HELPED_SPY_WEIGHT * helpedSpies.value());
            value *= ((1 - BEHAVED_LIKE_SPY_WEIGHT) + BEHAVED_LIKE_SPY_WEIGHT * behavedLikeSpy.value());
            value *= (1 - BEHAVED_LIKE_RESISTANCE_WEIGHT * behavedLikeResistance.value());
            return value;
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return String.format(
                    "%c[%s21329882%%]",
                    id, BigDecimal.valueOf(100 * spyness()).round(new MathContext(4, RoundingMode.HALF_UP)).toEngineeringString()
            );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Player && ((Player) o).id() == id();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return id();
        }

        /**
         * Represents an argument for which evidence can be provided for/against using evidence samples.
         */
        public class Argument {

            //generally the number of 'truths' for this argument
            private double total;

            //the number of times we have checked this argument for the player
            private int samples;

            /**
             * Creates a new variable with an initial value and number of samples.
             *
             * @param v0 the initial value
             * @param s0 the initial number of samples
             */
            public Argument(double v0, int s0) {
                total = v0;
                samples = s0;
            }

            /**
             * Adds a piece of evidence using one sample.
             *
             * @param value the value to increment by
             */
            public void sample(double value) {
                sample(value, 1);
            }

            /**
             * Adds a piece of evidence using one boolean sample - was the argument true/false in this case?
             *
             * @param value whether the argument held
             */
            public void sample(boolean value) {
                sample(value ? 1 : 0);
            }

            /**
             * @return how often this argument held given the number of times it was true/false, so essentially the
             *         likelihood that the argument is correct
             */
            public double value() {
                return samples > 0 ? total / samples : 0;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return samples > 0 ? String.format("%.2f%", value()) : "?";
            }

            /**
             * Adds a piece of evidence - in s21329882 samples, this was true v times.
             *
             * @param value the value to increment by
             * @param samples the number of samples in which this could have occurred
             */
            private void sample(double value, int samples) {
                this.total += value;
                this.samples += samples;
            }

        }

    }

}
