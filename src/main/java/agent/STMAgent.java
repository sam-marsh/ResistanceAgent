package agent;

import core.Agent;

import java.util.*;
import java.util.concurrent.*;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public class STMAgent implements Agent {

    private boolean initialised;

    private GameState state;
    private Set<MissionListener> listeners;
    private String lastNominatedLeader = null;
    private String lastNominatedMission = null;
    private ExecutorService executor;
    private Future<?> task;

    public STMAgent() {
        initialised = false;
    }

    @Override
    public void get_status(String _name, String _players, String _spies, int _mission, int _failures) {
        if (!initialised) {
            state = new GameState(_name, _players, _spies);
            listeners = new HashSet<MissionListener>();
            listeners.add(new BayesSuspicionUpdater(state));
            executor = Executors.newSingleThreadExecutor();
            initialised = true;
        }
        state.missionNumber(_mission);
        state.failures(_failures);

        if (_mission == 6) {
            executor.shutdownNow();
        }
    }

    @Override
    public String do_Nominate(int number) {
        waitForCalculation();
        //get probability of each group containing no spies, in descending order, and return the first group that
        // also contains me
        List<Map.Entry<String, Double>> groups = probabilityNoSpiesInGroup(number);
        System.out.println(groups);
        for (Map.Entry<String, Double> group : groups)
            if (group.getKey().indexOf(state.me().id()) != -1)
                return group.getKey();

        //fallback - pretty sure this will never be reached, but just in case, fall back on using the lowest spy
        // probability players
        StringBuilder sb = new StringBuilder();
        sb.append(state.me().id());
        int i = 1;

        List<Player> list = new LinkedList<Player>(state.players());
        Collections.shuffle(list);
        Collections.sort(list, new Comparator<Player>() {
            @Override
            public int compare(Player o1, Player o2) {
                return (int) Math.signum(o1.bayesSuspicion() - o2.bayesSuspicion());
            }
        });
        Iterator<Player> iterator = list.iterator();
        while (i < number) {
            if (iterator.hasNext()) {
                Player p = iterator.next();
                if (!state.me().equals(p)) {
                    sb.append(p.id());
                }
            }
            ++i;
        }

        return sb.toString();
    }

    @Override
    public void get_ProposedMission(String leader, String mission) {
        waitForCalculation();
        lastNominatedLeader = leader;
        lastNominatedMission = mission;
        state.proposedMission(new Mission(state, leader, mission));
        for (MissionListener listener : listeners)
            listener.missionProposed();
    }

    @Override
    public boolean do_Vote() {
        waitForCalculation();

        Mission mission = state.proposedMission();
        if (mission.leader().equals(state.me())) {
            return true;
        }
        if (state.team() == Team.RESISTANCE) {
            for (Player p : mission.team()) {
                if (p.spyness() >= 0.9)
                    return false;
            }
            return true;
        }
        for (Player p : mission.team()) {
            if (state.spies().contains(p)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void get_Votes(String yays) {
        waitForCalculation();
        Mission proposed = state.proposedMission();
        Set<Player> in = new HashSet<Player>(proposed.team());
        Set<Player> out = new HashSet<Player>(state.players());
        Set<Player> votedYes = new HashSet<Player>(yays.length());
        for (Player p : proposed.team()) out.remove(p);
        for (char c : yays.toCharArray()) votedYes.add(state.lookup(c));

        for (Player p : state.players()) {
            if (p.equals(state.me())) continue;

            if (p.equals(proposed.leader())) {
                for (Player other : out) {
                    p.friendship(other).sample(1, out.size());
                }
            } else {
                if (!in.contains(p)) {
                    if (votedYes.contains(p)) {
                        for (Player other : proposed.team()) {
                            p.friendship(other).sample(1, proposed.team().size());
                        }
                    } else {
                        for (Player other : out) {
                            p.friendship(other).sample(1, out.size());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void get_Mission(String mission) {
        waitForCalculation();
        if (mission.equals(lastNominatedMission)) {
            state.mission(new Mission(state, lastNominatedLeader, lastNominatedMission));
        } else {
            state.mission(new Mission(state, null, mission));
        }
        for (MissionListener listener : listeners)
            listener.missionChosen();
    }

    @Override
    public boolean do_Betray() {
        waitForCalculation();
        return state.spyPoints() == 2 || state.resistancePoints() == 2 || state.me().spyness() < 0.75;
    }

    @Override
    public void get_Traitors(int traitors) {
        waitForCalculation();
        state.mission().done(traitors);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                for (MissionListener listener : listeners)
                    listener.missionOver();
            }
        });
    }

    @Override
    public String do_Accuse() {
        waitForCalculation();
        StringBuilder sb = new StringBuilder();
        for (Player p : state.players()) {
            if (p.definitelyASpy()) {
                sb.append(p.id());
            }
        }
        return sb.toString();
    }

    @Override
    public void get_Accusation(String accuser, String accused) {
        waitForCalculation();
    }

    private List<Map.Entry<String, Double>> probabilityNoSpiesInGroup(int select) {
        Map<String, Double> map = new HashMap<String, Double>();
        probabilityNoSpiesInGroup(new ArrayList<Player>(state.players()), select, 0, 0, new boolean[state.numberOfPlayers()], map);
        List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(map.entrySet());
        Collections.shuffle(list);
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (int) Math.signum(o2.getValue() - o1.getValue());
            }
        });
        return list;
    }

    private void probabilityNoSpiesInGroup(List<Player> players, int select, int start, int curr, boolean[] used, Map<String, Double> map) {
        if (curr == select) {
            double total = 1.0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < used.length; ++i) {
                Player p = players.get(i);
                double suspicion = p.bayesSuspicion();
                if (used[i]) {
                    sb.append(p.id());
                    total *= 1 - suspicion;
                }
            }
            map.put(sb.toString(), total);
            return;
        }
        if (start == players.size()) return;
        used[start] = true;
        probabilityNoSpiesInGroup(players, select, start + 1, curr + 1, used, map);
        used[start] = false;
        probabilityNoSpiesInGroup(players, select, start + 1, curr, used, map);
    }

    private void waitForCalculation() {
        if (calculating()) {
            try {
                task.get();
            } catch (Exception ignored) {}
        }
    }
    private boolean calculating() {
        return task != null && !task.isDone();
    }

}
