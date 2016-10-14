package agent;

import core.Agent;

import java.util.*;

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

    public STMAgent() {
        initialised = false;
    }

    @Override
    public void get_status(String _name, String _players, String _spies, int _mission, int _failures) {
        if (!initialised) {
            state = new GameState(_name, _players, _spies);
            listeners = new HashSet<MissionListener>();
            listeners.add(new BayesSuspicionUpdater(state));
            initialised = true;
        }
        state.missionNumber(_mission);
        state.failures(_failures);

        Map<String, Double> map = new HashMap<String, Double>(state.numberOfPlayers());
        printGroupSuspicion(new ArrayList<Player>(state.players()), state.numberOfSpies(), 0, 0, new boolean[state.numberOfPlayers()], map);
        List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (int) Math.signum(o1.getValue() - o2.getValue());
            }
        });
        for (Player p : state.players())
            System.out.print(p + " ");
        System.out.println();
    }

    @Override
    public String do_Nominate(int number) {
        Set<Player> nomination = new HashSet<Player>(number);
        nomination.add(state.me()); //always pick myself

        List<Player> players = new LinkedList<Player>();
        players.addAll(state.players());
        Collections.shuffle(players);
        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player o1, Player o2) {
                return (int) Math.signum(o1.spyness() - o2.spyness());
            }
        });
        while (nomination.size() < number) {
            nomination.add(players.remove(0));
        }

        StringBuilder sb = new StringBuilder(number);
        for (Player p : nomination) sb.append(p.id());

        return sb.toString();
    }

    @Override
    public void get_ProposedMission(String leader, String mission) {
        lastNominatedLeader = leader;
        lastNominatedMission = mission;
        state.proposedMission(new Mission(state, leader, mission));
        for (MissionListener listener : listeners)
            listener.missionProposed();
    }

    @Override
    public boolean do_Vote() {
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
        return state.spyPoints() == 2 || state.resistancePoints() == 2 || state.me().spyness() < 0.75;
    }

    @Override
    public void get_Traitors(int traitors) {
        state.mission().done(traitors);
        for (MissionListener listener : listeners)
            listener.missionOver();
    }

    @Override
    public String do_Accuse() {
        StringBuilder sb = new StringBuilder();
        for (Player p : state.players()) {
            if (p.spyness() >= 0.95) {
                sb.append(p.id());
            }
        }
        return sb.toString();
    }

    @Override
    public void get_Accusation(String accuser, String accused) {

    }

    private void printGroupSuspicion(List<Player> players, int spies, int start, int curr, boolean[] used, Map<String, Double> map) {
        if (curr == spies) {
            double total = 1.0;
            String s = "";
            for (int i = 0; i < used.length; ++i) {
                Player p = players.get(i);
                double suspicion = p.bayesSuspicion();
                if (used[i]) {
                    s += p.id();
                    total *= suspicion;
                } else {
                    total *= (1 - suspicion);
                }
            }
            map.put(s, total);
            return;
        }
        if (start == players.size()) return;
        used[start] = true;
        printGroupSuspicion(players, spies, start + 1, curr + 1, used, map);
        used[start] = false;
        printGroupSuspicion(players, spies, start + 1, curr, used, map);
    }

}
