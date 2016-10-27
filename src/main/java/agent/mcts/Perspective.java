package agent.mcts;

import java.util.*;

/**
 * Holds bayes suspicions for all other players from the perspective of a resistance member
 */
public class Perspective {

    private final char me;
    private final Map<Character, Double> suspicion;
    private final char[] players;
    private final int numSpies;

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

    public Perspective(Perspective perspective) {
        this.me = perspective.me;
        this.suspicion = new HashMap<Character, Double>(perspective.suspicion);
        this.players = perspective.players;
        this.numSpies = perspective.numSpies;
    }

    public double lookup(char c) {
        return suspicion.get(c);
    }

    @Override
    public String toString() {
        return suspicion.toString();
    }

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

    private double betrayProbability(int spiesOnMission) {
        return 0.95 / spiesOnMission;
    }

    private double bayes(double pa, double pb, double pba) {
        if (pa == 0 || pba == 0) return 0;
        return pa * pba / pb;
    }

    private double computeProbabilityOfMissionSabotages(char[] mission, int traitors) {
        return computeProbabilityOfMissionSabotages(mission, numSpies, traitors, 0, 0, new boolean[players.length]);
    }

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

    private boolean contains(char[] array, char c) {
        for (char other : array)
            if (other == c)
                return true;
        return false;
    }

}
