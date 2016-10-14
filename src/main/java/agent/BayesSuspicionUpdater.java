package agent;

import java.util.*;

/**
 * This updates spy probabilities after each round via Bayes rule.
 */
public class BayesSuspicionUpdater implements MissionListener {

    //the game context for accessing/updating original player spy probabilities
    private final GameState state;

    /**
     * Creates a new listener for updating spy probabilities after each mission.
     * @param state the game context
     */
    public BayesSuspicionUpdater(GameState state) {
        this.state = state;
    }

    @Override
    public void missionProposed() {
        //ignore
    }

    @Override
    public void missionChosen() {
        //ignore
    }

    @Override
    public void missionOver() {
        //remove line below - still extremely useful to update probabilities when mission succeeds, since
        // if no sabotages occur it is more likely that there were no spies on the team
        //if (traitors == 0) return;

        //need a temporary map to store new probabilities, since need all spy probabilities to remain constant while
        // computing the new ones
        Map<Player, Double> updated = new HashMap<Player, Double>(state.players().size());

        //need to create new list to hold the collection of players since need to access by index - bit annoying
        List<Player> players = new LinkedList<Player>(state.players());

        //P(B) - probability of cards being dealt - is constant for all players so can compute once
        double pb = computeProbabilityOfMissionSabotages(players);

        for (Player p : state.players()) {
            //P(A) - original probability of being a spy
            double pa = p.bayesSuspicion();

            //P(B|A) - probability of cards being dealt given that the player is a spy - just temporarily set
            // spy probability of player to 1
            p.bayesSuspicion(1.0);
            double pba = computeProbabilityOfMissionSabotages(players);
            p.bayesSuspicion(pa);

            //add new probability to map
            updated.put(p, bayes(pa, pb, pba));
        }

        //computation is done - update probabilities from map
        for (Map.Entry<Player, Double> entry : updated.entrySet()) {
            entry.getKey().bayesSuspicion(entry.getValue());
        }
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
                if (state.mission().team().contains(p) && sabotaged[i])
                    numSabotaged++;
            }
            if (numSabotaged != state.mission().traitors()) return 0;

            List<Player> spiesOnMission = new LinkedList<Player>();
            for (Player p : spies) if (state.mission().team().contains(p)) spiesOnMission.add(p);

            double total = 1.0;
            for (int i = 0; i < sabotaged.length; ++i) {
                Player p = spies.get(i);
                if (state.mission().team().contains(p)) {
                    if (sabotaged[i]) {
                        //spy on mission and spy sabotaged
                        total *= p.bayesSuspicion() * p.likelihoodToBetray(state.mission(), spiesOnMission);
                    } else {
                        //spy on mission but didn't sabotage
                        total *= p.bayesSuspicion() * (1 - p.likelihoodToBetray(state.mission(), spiesOnMission));
                    }
                } else {
                    //spy not on mission
                    total *= p.bayesSuspicion();
                }
            }

            for (Player p : state.players()) {
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

}
