package agent;

import java.util.*;

/**
 * Author: Sam Marsh
 * Date: 11/10/2016
 */
public class BayesSuspicionUpdater implements MissionListener {

    private final GameState state;

    public BayesSuspicionUpdater(GameState state) {
        this.state = state;
    }

    @Override
    public void missionProposed() {
        //ignore
    }

    @Override
    public void missionChosen() {

    }

    @Override
    public void missionOver() {
        int traitors = state.mission().traitors();
        int spies = state.numberOfSpies();

        if (traitors == 0) return;

        List<Player> in = new ArrayList<Player>(state.mission().team());

        List<Player> out = new ArrayList<Player>(state.players());
        for (Player p : in) out.remove(p);

        Map<Player, Double> updated = new HashMap<Player, Double>(state.players().size());

        double pbIn = calculate(in, traitors);
        double pbOut = calculate(out, spies - traitors);

        for (Player p : state.players()) {
            double pa = p.bayesSuspicion();
            double pb;
            double pba;
            if (in.contains(p)) {
                pb = pbIn;
                List<Player> removed = new ArrayList<Player>(in);
                removed.remove(p);
                pba = calculate(removed, traitors - 1);
            } else {
                pb = pbOut;
                if (spies - traitors == 0) {
                    pba = 0;
                } else {
                    List<Player> removed = new ArrayList<Player>(out);
                    removed.remove(p);
                    pba = calculate(removed, spies - traitors - 1);
                }
            }
            updated.put(p, bayes(pa, pb, pba));
        }

        for (Map.Entry<Player, Double> entry : updated.entrySet()) {
            entry.getKey().bayesSuspicion(entry.getValue());
        }
    }

    private double calculate(List<Player> team, int traitors) {
        return calculate(team, traitors, 0, 0, new boolean[team.size()]);
    }

    private double calculate(List<Player> team, int traitors, int start, int curr, boolean[] used) {
        if (curr == traitors) {
            double total = 1.0;
            for (int i = 0; i < used.length; ++i) {
                Player p = team.get(i);
                double suspicion = p.bayesSuspicion();
                if (used[i]) {
                    total *= suspicion;
                } else {
                    total *= (1 - suspicion);
                }
            }
            return total;
        }
        if (start == team.size()) return 0;
        used[start] = true;
        double tmp = calculate(team, traitors, start + 1, curr + 1, used);
        used[start] = false;
        return tmp + calculate(team, traitors, start + 1, curr, used);
    }

    private double bayes(double pa, double pb, double pba) {
        if (pa == 0 || pba == 0) return 0;
        return pa * pba / pb;
    }

}
