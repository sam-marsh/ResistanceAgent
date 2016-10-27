package s21329882;

import cits3001_2016s2.Agent;

import java.util.*;

/**
 * Author: Sam Marsh
 * Date: 15/10/2016
 */
public class BayesResistanceAgent implements Agent {

    private boolean initialised;

    private Map<Collection<Player>, Double> suspectsPair;
    private GameState state;
    private ResistancePerspective perspective;
    private String lastNominatedMission = null;

    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            state = new GameState(players, spies);
            perspective = new ResistancePerspective(state, name, players, spies);
            suspectsPair = new HashMap<Collection<Player>, Double>();
        }
        state.missionNumber(mission);
        state.failures(failures);

        if (initialised)
            updateSuspectsPair();
    }

    private Map.Entry<Collection<Player>, Double> getBadPair() {
        Map.Entry<Collection<Player>, Double> max = null;
        for (Map.Entry<Collection<Player>, Double> entry : suspectsPair.entrySet()) {
            if (max == null || entry.getValue() * randBetween(0.9, 1) > max.getValue()) {
                max = entry;
            }
        }
        if (max == null) {
            throw new NullPointerException();
        }
        return max;
    }

    private Collection<Player> getGood() {
        List<Player> players = new ArrayList<Player>(perspective.others());
        Map.Entry<Collection<Player>, Double> bad = getBadPair();
        if (bad.getValue() > 0) {
            for (Player p : bad.getKey()) {
                players.remove(p);
            }
        }
        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player o1, Player o2) {
                return (int) Math.signum(o1.spyness() - o2.spyness());
            }
        });
        return players;
    }

    private double randBetween(double x, double y) {
        return Math.random() * (y - x) + x;
    }

    private void updateSuspectsPair() {
        for (Map.Entry<Collection<Player>, Double> entry : suspectsPair.entrySet()) {
            double estimate = 1;
            for (Player player : entry.getKey()) {
                estimate *= player.spyness();
            }
            if (estimate < 0.99) {
                double u = 1;
                for (Player player : entry.getKey()) {
                    for (Player other : entry.getKey()) {
                        if (!player.equals(other)) {
                            u *= player.friendship(other).estimate();
                        }
                    }
                }
                double v = (0.5 + 0.5 * u);
                u = 1;
                for (Player player : entry.getKey()) {
                    u *= player.getSupportSuspect().estimate();
                }
                v *= (0.75 + 0.25 * u);
                v *= estimate;
                u = 1;
                for (Player player : entry.getKey()) {
                    u *= player.suspiciousActions().estimate();
                }
                v *= (0.4 + 0.6 * u);
                u = 1;
                for (Player player : entry.getKey()) {
                    u *= player.getPossibleGoodActions().estimate();
                }
                v *= (1 - 0.1 * u);
                entry.setValue(v);
            }
        }
    }

    @Override
    public String do_Nominate(int number) {
        if (!initialised) {
            List<Player> others = perspective.others();
            combinations(state.numberOfSpies(), 0, 0, others, new boolean[others.size()], suspectsPair);
            initialised = true;

            StringBuilder sb = new StringBuilder();
            sb.append(perspective.me().id());
            Collections.shuffle(perspective.others());
            for (Player p : perspective.others()) {
                if (sb.length() == number) break;
                sb.append(p.id());
            }
            return sb.toString();
        }

        Collection<Player> good = getGood();

        StringBuilder sb = new StringBuilder();
        sb.append(perspective.me().id());
        for (Player p : good) if (sb.length() < number) sb.append(p.id());

        if (sb.length() < number) throw new IllegalArgumentException();

        return sb.toString();
    }

    @Override
    public void get_ProposedMission(String leader, String mission) {
        lastNominatedMission = mission;
        state.proposedMission(new GameState.Mission(leader, mission));

        perspective.lookup(leader.charAt(0)).suspiciousActions().sample(mission.contains(leader));
        updateSuspectsPair();
    }

    @Override
    public boolean do_Vote() {
        if (!initialised) {
            List<Player> others = perspective.others();
            combinations(state.numberOfSpies(), 0, 0, others, new boolean[others.size()], suspectsPair);
            initialised = true;
            return true;
        }

        GameState.Mission mission = state.proposedMission();
        if (mission.leader() == perspective.me().id()) {
            return true;
        }

        if (mission.team().size() >= 3 && !mission.team().contains(perspective.me().id())) {
            return false;
        }

        Map.Entry<Collection<Player>, Double> bad = getBadPair();
        Collection<Character> coll = new ArrayList<Character>();
        for (Player p : bad.getKey())
            coll.add(p.id());

        return !intersection(coll, mission.team());
    }

    private <T> boolean intersection(Collection<T> smaller, Collection<T> larger) {
        for (T t : smaller)
            if (larger.contains(t))
                return true;
        return false;
    }

    private String yays;

    @Override
    public void get_Votes(String yays) {
        this.yays = yays;
        GameState.Mission proposed = state.proposedMission();
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

        for (Player p : perspective.players())
            p.getPossibleGoodActions().sample(contains(lastNominatedMission, p.id()) && !contains(yays, p.id()));

        updateSuspectsPair();
    }

    @Override
    public void get_Mission(String mission) {
        if (mission.equals(lastNominatedMission)) {
            state.mission(state.proposedMission());
        } else {
            state.mission(new GameState.Mission(null, mission));
        }
    }

    @Override
    public boolean do_Betray() {
        throw new AssertionError();
    }

    @Override
    public void get_Traitors(int traitors) {
        state.mission().done(traitors);

        for (Player player : perspective.players()) {
            if (!player.equals(perspective.me())) {
                int val = ((contains(yays, player.id()) && traitors > 0) || (!contains(yays, player.id()) && traitors == 0)) ? 1 : 0;
                player.getSupportSuspect().sample(val);
            }
        }

        perspective.updateSuspicion();
        updateSuspectsPair();
    }

    private boolean contains(String s, char c) {
        return s.indexOf(c) != -1;
    }

    @Override
    public String do_Accuse() {
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

    private void combinations(int spies, int start, int curr, List<Player> others, boolean[] used, Map<Collection<Player>, Double> map) {
        if (curr == spies) {
            Collection<Player> collection = new ArrayList<Player>(spies);
            for (int i = 0; i < used.length; ++i) {
                if (used[i])
                    collection.add(others.get(i));
            }
            map.put(collection, 0.0);
            return;
        }
        //another base case - finished
        if (start == others.size()) return;

        //use the player at the start index in the team and recurse
        used[start] = true;
        combinations(spies, start + 1, curr + 1, others, used, map);

        //don't use the player at the start index in the team and recurse
        used[start] = false;
        combinations(spies, start + 1, curr, others, used, map);
    }

}
