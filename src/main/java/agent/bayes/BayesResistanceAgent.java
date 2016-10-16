package agent.bayes;

import agent.*;
import core.Agent;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Author: Sam Marsh
 * Date: 15/10/2016
 */
public class BayesResistanceAgent implements Agent {

    private boolean initialised;

    private GameState state;
    private ResistancePerspective perspective;
    private String lastNominatedLeader = null;
    private String lastNominatedMission = null;
    private ExecutorService executor;
    private Future<?> task;

    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            state = new GameState(players, spies);
            perspective = new ResistancePerspective(state, name, players, spies);
            executor = Executors.newSingleThreadExecutor();
            initialised = true;
        }
        state.missionNumber(mission);
        state.failures(failures);

        if (state.gameOver()) {
            waitForCalculation();
            executor.shutdownNow();
        }

    }

    @Override
    public String do_Nominate(int number) {
        waitForCalculation();
        //get probability of each group containing no spies, in descending order, and return the first group that
        // also contains me
        List<Map.Entry<String, Double>> groups = probabilityNoSpiesInGroup(number);
        for (Map.Entry<String, Double> group : groups)
            if (group.getKey().indexOf(perspective.me().id()) != -1)
                return group.getKey();

        //fallback - pretty sure this will never be reached, but just in case, fall back on using the lowest spy
        // probability players
        StringBuilder sb = new StringBuilder();
        sb.append(perspective.me().id());
        int i = 1;

        List<Player> list = new LinkedList<Player>(perspective.players());
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
                if (!perspective.me().equals(p)) {
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
    }

    @Override
    public boolean do_Vote() {
        waitForCalculation();

        Mission mission = state.proposedMission();
        if (mission.leader() == perspective.me().id()) {
            return true;
        }

        if (!mission.team().contains(perspective.me().id()))
            return false;

        for (Character c : mission.team()) {
            if (perspective.lookup(c).spyness() >= 0.9)
                return false;
        }

        return true;
    }

    @Override
    public void get_Votes(String yays) {
        waitForCalculation();
        Mission proposed = state.proposedMission();
        Set<Character> in = new HashSet<Character>(proposed.team());
        Set<Character> out = new HashSet<Character>(state.numberOfPlayers());
        Set<Character> votedYes = new HashSet<Character>(yays.length());
        for (Character p : proposed.team()) out.remove(p);
        for (char c : yays.toCharArray()) votedYes.add(c);

        for (Character c : state.players()) {
            Player p = perspective.lookup(c);
            if (c.equals(perspective.me().id())) continue;

            if (c.equals(proposed.leader())) {
                for (Character other : out) {
                    perspective.lookup(other).friendship(p).sample(1, out.size());
                }
            } else {
                if (!in.contains(c)) {
                    if (votedYes.contains(c)) {
                        for (Character other : proposed.team()) {
                            p.friendship(perspective.lookup(other)).sample(1, proposed.team().size());
                        }
                    } else {
                        for (Character other : out) {
                            p.friendship(perspective.lookup(other)).sample(1, out.size());
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
    }

    @Override
    public boolean do_Betray() {
        throw new AssertionError();
    }

    @Override
    public void get_Traitors(int traitors) {
        waitForCalculation();
        state.mission().done(traitors);

        task = executor.submit(new Runnable() {
            @Override
            public void run() {
                perspective.updateSuspicion();
            }
        });
    }

    @Override
    public String do_Accuse() {
        waitForCalculation();
        StringBuilder sb = new StringBuilder();
        for (Player p : perspective.players()) {
            if (p.definitelyASpy()) {
                sb.append(p.id());
            }
        }
        return sb.toString();
    }

    @Override
    public void get_Accusation(String accuser, String accused) {

    }

    private List<Map.Entry<String, Double>> probabilityNoSpiesInGroup(int select) {
        Map<String, Double> map = new HashMap<String, Double>();
        probabilityNoSpiesInGroup(new ArrayList<Player>(perspective.players()), select, 0, 0, new boolean[state.numberOfPlayers()], map);
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

    public GameState state() {
        return state;
    }

}
