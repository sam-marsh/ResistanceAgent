package agent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public class BayesSuspicionTracker implements GameEventListener {

    private Map<Character, Double> spyness;
    private char[] players;
    private char[] currentTeam;
    private int nspies;

    public BayesSuspicionTracker(String players, int nspies, String definitelyNotSpy) {
        this.spyness = new HashMap<Character, Double>();
        this.players = players.toCharArray();
        this.currentTeam = null;
        this.nspies = nspies;
        for (char c : this.players) {
            spyness.put(c, (double) nspies / players.replace(definitelyNotSpy, "").length());
        }
        if (!definitelyNotSpy.equals("")) {
            spyness.put(definitelyNotSpy.charAt(0), 0.0);
        }
    }

    public double spyness(char player) {
        return spyness.get(player);
    }

    @Override
    public void status(String name, String players, String spies, int mission, int failures) {

    }

    @Override
    public void proposedMission(String leader, String mission) {

    }

    @Override
    public void votes(String yays) {

    }

    @Override
    public void mission(String mission) {
        currentTeam = mission.toCharArray();
    }

    private boolean contains(char[] array, char c) {
        for (char tmp : array)
            if (tmp == c)
                return true;
        return false;
    }

    private char[] remove(char[] array, char c) {
        char[] tmp = new char[array.length];
        int i, j;
        for (i = j = 0; j < array.length; ++j) {
            if (array[j] != c)
                tmp[i++] = array[j];
        }
        return Arrays.copyOf(tmp, i);
    }

    @Override
    public void traitors(int traitors) {
        if (traitors == 0) return;
        char[] in = currentTeam;
        char[] out = new char[players.length - in.length];
        int tmp = 0;
        for (char c : players) {
            if (!contains(in, c)) {
                out[tmp++] = c;
            }
        }
        Map<Character, Double> spyness_tmp = new HashMap<Character, Double>();
        double p_b_in = calculate(in, traitors);
        double p_b_out = calculate(out, nspies - traitors);
        for (char c : players) {
            double p_a = spyness.get(c);
            double p_b;
            double p_b_a;
            if (contains(in, c)) {
                p_b = p_b_in;
                p_b_a = calculate(remove(in, c), traitors - 1);
            } else {
                p_b = p_b_out;
                if (nspies - traitors == 0) {
                    p_b_a = 0;
                } else {
                    p_b_a = calculate(remove(out, c), nspies - traitors - 1);
                }
            }
            spyness_tmp.put(c, bayes(p_a, p_b, p_b_a));
        }
        for (Map.Entry<Character, Double> entry : spyness_tmp.entrySet()) {
            spyness.put(entry.getKey(), entry.getValue());
        }
    }

    private double bayes(double p_a, double p_b, double p_b_a) {
        if (p_a == 0 || p_b_a == 0)
            return 0;
        return p_a * p_b_a / p_b;
    }

    private double calculate(char[] team, int traitors) {
        return calculate(team, traitors, 0, 0, new boolean[team.length]);
    }

    private double calculate(char[] team, int traitors, int start, int currLen, boolean[] used) {
        if (currLen == traitors) {
            int j = 0;
            char[] spyCombination = new char[traitors];
            for (int i = 0; i < team.length; ++i) {
                if (used[i]) {
                    spyCombination[j++] = team[i];
                }
            }
            double total = 1.0;
            for (char c : team) {
                if (contains(spyCombination, c))
                    total *= spyness.get(c);
                else
                    total *= 1 - spyness.get(c);
            }
            return total;
        }
        if (start == team.length)
            return 0;
        used[start] = true;
        double tmp = calculate(team, traitors, start + 1, currLen + 1, used);
        used[start] = false;
        return tmp + calculate(team, traitors, start + 1, currLen, used);
    }

    @Override
    public void accusation(String accuser, String accused) {

    }
}
