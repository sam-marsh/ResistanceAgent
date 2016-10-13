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

        Mission mission = state.mission();

        List<Player> in = new ArrayList<Player>(state.mission().team());

        List<Player> out = new ArrayList<Player>(state.players());
        for (Player p : in) out.remove(p);

        Map<Player, Double> updated = new HashMap<Player, Double>(state.players().size());

        List<Player> pl = new LinkedList<Player>(state.players());
        double pbIn = calculate(mission, pl, in, spies, traitors);

        for (Player p : state.players()) {
            double pa = p.bayesSuspicion();
            double pb;
            double pba;
            pb = pbIn;
            p.bayesSuspicion(1.0);
            pba = calculate(mission, pl, in, spies, traitors);
            p.bayesSuspicion(pa);
            updated.put(p, bayes(pa, pb, pba));
        }

        for (Map.Entry<Player, Double> entry : updated.entrySet()) {
            entry.getKey().bayesSuspicion(entry.getValue());
        }
    }

    private double advancedCalculate(Mission mission, Collection<Player> players, List<Player> team, List<Player> spies, int sabotaged, int start, int curr, boolean spySabotaged[]) {
        if (curr == sabotaged) {
            int numSabotaged = 0;
            for (int i = 0; i < spySabotaged.length; ++i) {
                Player p = spies.get(i);
                if (team.contains(p) && spySabotaged[i])
                    numSabotaged++;
            }
            if (numSabotaged != sabotaged) return 0;
            double total = 1.0;
            for (int i = 0; i < spySabotaged.length; ++i) {
                Player p = spies.get(i);
                if (team.contains(p)) {
                    if (spySabotaged[i])
                        total *= p.bayesSuspicion() * p.likelihoodToBetray(mission);
                    else
                        total *= p.bayesSuspicion() * (1 - p.likelihoodToBetray(mission));
                } else {
                    total *= p.bayesSuspicion();
                }
            }
            for (Player p : players) {
                if (!spies.contains(p)) {
                    total *= (1 - p.bayesSuspicion());
                }
            }
            return total;
        }
        if (start == spies.size()) return 0;
        spySabotaged[start] = true;
        double tmp = advancedCalculate(mission, players, team, spies, sabotaged, start + 1, curr + 1, spySabotaged);
        spySabotaged[start] = false;
        return tmp + advancedCalculate(mission, players, team, spies, sabotaged, start + 1, curr, spySabotaged);
    }

    private double calculate(Mission mission, List<Player> players, List<Player> team, int nspies, int traitors) {
        return calculate(mission, players, team, nspies, traitors, 0, 0, new boolean[players.size()]);
    }

    private double calculate(Mission mission, List<Player> players, List<Player> team, int nspies, int traitors, int start, int curr, boolean[] used) {
        if (curr == nspies) {
            List<Player> spies = new LinkedList<Player>();
            for (int i = 0; i < players.size(); ++i) if (used[i]) spies.add(players.get(i));
            return advancedCalculate(mission, players, team, spies, traitors, 0, 0, new boolean[spies.size()]);
        }
        if (start == players.size()) return 0;
        used[start] = true;
        double tmp = calculate(mission, players, team, nspies, traitors, start + 1, curr + 1, used);
        used[start] = false;
        return tmp + calculate(mission, players, team, nspies, traitors, start + 1, curr, used);
    }

    private double bayes(double pa, double pb, double pba) {
        if (pa == 0 || pba == 0) return 0;
        return pa * pba / pb;
    }

}
