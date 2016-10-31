package s21324325;

import s21329882.GameState;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Holds bayes suspicions for all other players from the perspective of a resistance member. Lightweight version
 * of {@link s21329882.ResistancePerspective}.
 */
public class Perspective {

    //who i am
    private final char me;

    //holds Bayesian-updated probabilities of spyness for each player
    private final Map<Character, Double> suspicion;

    //every player in the game
    private final char[] players;

    //the number of spies in the game
    private final int numSpies;

    /**
     * Creates a new perspective on the game from the point of view of a resistance member.
     * Uses Bayes' rule to update probabilities that other players are spies.
     *
     * @param me who i am
     * @param players all players
     * @param spies number of spies
     */
    public Perspective(char me, char[] players, int spies) {
        this.me = me;
        this.suspicion = new HashMap<Character, Double>();
        this.players = players;
        this.numSpies = spies;
        double initial = (double) spies / (players.length - 1);
        for (char id : players) {
            suspicion.put(id, id == me ? 0 : initial);
        }
    }

    /**
     * Clones a perspective.
     *
     * @param perspective the perspective to clone
     */
    public Perspective(Perspective perspective) {
        this.me = perspective.me;
        this.suspicion = new HashMap<Character, Double>(perspective.suspicion);
        this.players = perspective.players;
        this.numSpies = perspective.numSpies;
    }

    /**
     * @param c the player identifier
     * @return the probability that the player is a spy
     */
    public double lookup(char c) {
        return suspicion.get(c);
    }

    /**
     * Updates the suspicion for each player based on the current round evidence - i.e. number of sabotages.
     *
     * @param mission the players on the mission
     * @param traitors the number of traitors
     */
    public void update(char[] mission, int traitors) {
        Map<Character, Double> updated = new HashMap<Character, Double>(suspicion.size());
        for (char id : players) {
            double pa = suspicion.get(id);
            suspicion.put(id, 1.0);
            double pba = computeProbabilityOfMissionSabotages(mission, traitors);
            suspicion.put(id, pa);
            updated.put(id, bayes(pa, 1.0, pba));
        }
        double total = 0.0;
        for (Map.Entry<Character, Double> entry : updated.entrySet()) {
            total += entry.getValue();
        }
        for (Map.Entry<Character, Double> entry : updated.entrySet()) {
            double newValue = entry.getValue() * numSpies / total;
            suspicion.put(entry.getKey(), Math.max(0, Math.min(newValue, 1)));
        }
    }

    /**
     * Opponent model - the probability of a spy sabotaging given the chance.
     *
     * @param spiesOnMission the number of spies on the mission
     * @return the probability of a spy sabotaging a mission
     */
    private double betrayProbability(int spiesOnMission) {
        return 0.95 / spiesOnMission;
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
     * @param mission the array of players in the mission
     * @param traitors the number of sabotages
     * @return the probability of the mission resulting in the number of sabotages that occurred
     */
    private double computeProbabilityOfMissionSabotages(char[] mission, int traitors) {
        return computeProbabilityOfMissionSabotages(mission, numSpies, traitors, 0, 0, new boolean[players.length]);
    }
    /**
     * The recursive method for {@link #computeProbabilityOfMissionSabotages(char[], int)}.
     *
     * @param mission the players on the mission
     * @param numSpies the total number of spies
     * @param traitors the number of sabotages
     * @param start recursive parameter to help iterate over combinations - pass 0
     * @param curr recursive parameter to help iterate over combinations - pass 0
     * @param spy recursive parameter to help iterate over combinations - pass a boolean array filled with the
     *             value {@code false} of size {@link GameState#numberOfPlayers()}
     * @return the probability of the mission resulting in the number of sabotages that occurred
     */
    private double computeProbabilityOfMissionSabotages(char[] mission, int numSpies, int traitors, int start, int curr, boolean[] spy) {
        //base case - have reached the correct combination size
        if (curr == numSpies) {
            //create list of spies and then iterate over all possibilities for which spies did/didn't sabotage
            List<Character> spies = new LinkedList<Character>();
            for (int i = 0; i < players.length; ++i) if (spy[i]) spies.add(players[i]);
            return sumSabotageCombinations(spies, mission, traitors, 0, 0, new boolean[spies.size()]);
        }

        //base case - ensure not over the array size
        if (start == players.length) return 0;

        //recurse - set the player in the first index to be a spy
        spy[start] = true;
        double tmp = computeProbabilityOfMissionSabotages(mission, numSpies, traitors, start + 1, curr + 1, spy);

        //recurse - set the player in the first index to be a true resistance member
        spy[start] = false;
        return tmp + computeProbabilityOfMissionSabotages(mission, numSpies, traitors, start + 1, curr, spy);
    }

    /**
     * If a spy is in a team, it has some probability to sabotage the mission. This method iterates over a given
     * spy combination and considers all combinations of which spies sabotaged/didn't sabotage. It sums the probabilities
     * and returns the total probability for the given number of sabotages to have occurred in a mission, assuming a certain
     * spy combination.
     *
     * @param spies the assumed list of spies (of size {@link s21329882.GameState#numberOfSpies()})
     * @param mission the mission that just occurred
     * @param traitors the number of sabotages
     * @param start recursive parameter to help iterate over combinations - pass 0
     * @param curr recursive parameter to help iterate over combinations - pass 0
     * @param sabotaged recursive parameter to help iterate over combinations - pass a boolean array filled with the
     *                     value {@code false} of size {@link s21329882.GameState#numberOfSpies()}
     * @return the total probability for a given set of spies to have sabotaged the number of times that the mission
     * was sabotaged
     */
    private double sumSabotageCombinations(List<Character> spies, char[] mission, int traitors, int start, int curr, boolean sabotaged[]) {
        //base case - have reached the correct combination size
        if (curr == traitors) {

            //ensure that the number of sabotages in this combination caused by spies IN THE TEAM corresponds to the
            // number of sabotages that actually occurred
            int numSabotaged = 0;
            for (int i = 0; i < sabotaged.length; ++i) {
                if (contains(mission, spies.get(i)) && sabotaged[i])
                    numSabotaged++;
            }
            if (numSabotaged != traitors) return 0;

            List<Character> spiesOnMission = new LinkedList<Character>();
            for (Character c : spies) if (contains(mission, c)) spiesOnMission.add(c);

            double total = 1.0;
            for (int i = 0; i < sabotaged.length; ++i) {
                Character c = spies.get(i);
                double s = suspicion.get(c);
                if (contains(mission, c)) {
                    if (sabotaged[i]) {
                        //spy on mission and spy sabotaged
                        total *= s * betrayProbability(spiesOnMission.size());
                    } else {
                        //spy on mission but didn't sabotage
                        total *= s * (1 - betrayProbability(spiesOnMission.size()));
                    }
                } else {
                    //spy not on mission
                    total *= s;
                }
            }

            for (Character c : players) {
                if (!spies.contains(c)) {
                    //not a spy
                    total *= (1 - suspicion.get(c));
                }
            }

            return total;
        }

        //base case - ensure not over the array size
        if (start == sabotaged.length) return 0;

        //recurse - set the spy in the start index to have sabotaged
        sabotaged[start] = true;
        double tmp = sumSabotageCombinations(spies, mission, traitors, start + 1, curr + 1, sabotaged);

        //recurse - set the spy in the start index not to have sabotaged
        sabotaged[start] = false;
        return tmp + sumSabotageCombinations(spies, mission, traitors, start + 1, curr, sabotaged);
    }

    /**
     * Convenience method for determining if a character array contains a character.
     *
     * @param array the haystack
     * @param c the needle
     * @return whether the character array contains the specified character
     */
    private boolean contains(char[] array, char c) {
        for (char other : array)
            if (other == c)
                return true;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return suspicion.toString();
    }


}
