package agent.mcts.impl;

import agent.util.PermutationIterator;
import core.Agent;

import java.util.*;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class LogicalAgent implements Agent {

    private boolean initialised;
    private char[] players;
    private String spies;
    private char me;
    private Map<Integer, Character> indices;
    private Map<Character, Integer> reverse;
    private Map<Boolean[], Double> permutations;
    private int round;
    private int failures;
    private int tries;

    public LogicalAgent() {
        initialised = false;
        spies = null;
        permutations = null;
    }

    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            this.players = players.toCharArray();
            this.spies = spies;
            this.me = name.charAt(0);

            this.indices = new HashMap<Integer, Character>(this.players.length - 1);
            this.reverse = new HashMap<Character, Integer>(this.players.length - 1);
            int i = 0;
            for (char c : this.players) {
                if (c != me) {
                    reverse.put(c, i);
                    indices.put(i++, c);
                }
            }

            this.permutations = new HashMap<Boolean[], Double>();
            Boolean[] config = new Boolean[players.length() - 1];
            for (int j = 0; j < spies.length(); ++j) {
                config[j] = true;
            }
            for (int j = spies.length(); j < this.players.length - 1; ++j) {
                config[j] = false;
            }
            Iterator<Boolean[]> iterator = new PermutationIterator<Boolean>(Arrays.asList(config));
            while (iterator.hasNext()) {
                permutations.put(iterator.next(), 0.0);
            }

            tries = 0;

            initialised = true;
        }

        this.round = mission;
        this.failures = failures;

    }

    private double oracleSelection(char leader, String mission, Boolean config[]) {
        Set<Character> spies = spies(config);
        Set<Character> resistance = resistance(config);
        Set<Character> teamSpies = teamSpies(mission, spies);

        if (spies.contains(leader) && teamSpies.size() == 0) {
            return 1.0;
        } else if (teamSpies.size() >= 2) { //todo needs to work with larger teams where more can sabotage
            return 0.5;
        }
        return 0.0;
    }

    private double oracleVoting(String yays, Boolean[] config) {
        Set<Character> spies = spies(config);
        Set<Character> teamSpies = teamSpies(mission, spies);
        double score = 0.0;
        for (Character c : players) {
            boolean yay = yays.indexOf(c) != -1;
            if (teamSpies.contains(c) && yay && teamSpies.size() == 0) {
                score += 1;
            }
            if (teamSpies.contains(c) && !yay && teamSpies.size() == 1) {
                score += 1;
            }
            if (teamSpies.contains(c) && yay && teamSpies.size() > 1) {  //todo as above
                score += 0.5;
            }
            //todo another two here from github
        }
        return score;
    }

    private double oracleSabotages(String mission, int sabotaged, Boolean[] config) {
        Set<Character> spies = teamSpies(mission, spies(config));
        return (double) Math.max(0, sabotaged - spies.size());
    }

    private Boolean adviserVote(String team) {
        if (spies.indexOf(me) != -1) {
            Set<Character> s = new HashSet<Character>();
            for (char c : team.toCharArray())
                if (spies.indexOf(c) != -1)
                    s.add(c);
            if (s.size() > 0 && (failures == 2 || round == 5)) {
                return true;
            }
            if (tries == 4) { //todo offbyone?
                return true;
            }
            if (team.length() >= 3) {
                return team.indexOf(me) != -1;
            }
        } else {
            if (leader == me) {
                return true;
            }
            //todo offbyone?
            if (tries == 4) {
                return true;
            }
        }
        return null;
    }

    private Set<Character> teamSpies(String mission, Set<Character> spies) {
        Set<Character> teamSpies = new HashSet<Character>();
        for (char s : mission.toCharArray()) {
            if (spies.contains(s)) {
                teamSpies.add(s);
            }
        }
        return teamSpies;
    }

    private Set<Character> spies(Boolean[] config) {
        Set<Character> spies = new HashSet<Character>();
        for (int i = 0; i < config.length; ++i) {
            if (config[i]) {
                spies.add(indices.get(i));
            }
        }
        return spies;
    }

    private Set<Character> resistance(Boolean[] config) {
        Set<Character> resistance = new HashSet<Character>();
        for (int i = 0; i < config.length; ++i) {
            if (!config[i]) {
                resistance.add(indices.get(i));
            }
        }
        return resistance;
    }

    private Map.Entry<Boolean[], Double> likeliest() {
        List<Map.Entry<Boolean[], Double>> list = new ArrayList<Map.Entry<Boolean[], Double>>(permutations.entrySet());
        Collections.shuffle(list);
        Collections.sort(list, new Comparator<Map.Entry<Boolean[], Double>>() {
            @Override
            public int compare(Map.Entry<Boolean[], Double> o1, Map.Entry<Boolean[], Double> o2) {
                return (int) Math.signum(o1.getValue() - o2.getValue());
            }
        });
        return list.get(0);
    }

    @Override
    public String do_Nominate(int number) {
        Set<Character> set = sample(resistance(likeliest().getKey()), number - 1);
        set.add(me);
        StringBuilder sb = new StringBuilder();
        for (Character c : set) {
            sb.append(c);
        }
        return sb.toString();
    }

    private <T> Set<T> sample(Set<T> source, int number) {
        Set<T> set = new HashSet<T>();
        List<T> list = new ArrayList<T>(source);
        Collections.shuffle(list);
        int i = 0;
        while (i < number) {
            set.add(list.get(i));
            i++;
        }
        return set;
    }

    private String mission;
    private char leader;

    @Override
    public void get_ProposedMission(String leader, String mission) {
        this.mission = mission;
        this.leader = leader.charAt(0);
        ++tries;
        for (Map.Entry<Boolean[], Double> entry : permutations.entrySet()) {
            Double score = oracleSelection(this.leader, mission, entry.getKey());
            entry.setValue(entry.getValue() + score);
        }
    }

    @Override
    public boolean do_Vote() {
        Boolean advisor = adviserVote(mission);
        if (advisor != null) {
            return advisor;
        }
        List<Double> list = new ArrayList<Double>();
        for (Map.Entry<Boolean[], Double> entry : permutations.entrySet()) {
            Set<Character> spies = teamSpies(mission, spies(entry.getKey()));
            if (spies.size() == 0) {
                list.add(entry.getValue());
            }
        }
        if (list.isEmpty()) return false;
        double threshold = Double.MAX_VALUE;
        for (double value : permutations.values()) {
            if (value < threshold) threshold = value;
        }
        double min = Double.MAX_VALUE;
        for (double value : list) {
            if (value < min) min = value;
        }
        return min <= threshold;
    }

    @Override
    public void get_Votes(String yays) {
        for (Map.Entry<Boolean[], Double> entry : permutations.entrySet()) {
            Double score = oracleVoting(yays, entry.getKey());
            entry.setValue(entry.getValue() + score);
        }
    }

    @Override
    public void get_Mission(String mission) {
        tries = 0;
    }

    @Override
    public boolean do_Betray() {
        if (failures == 2 || round == 5) {
            return true;
        }
        if (mission.length() == 2 && round == 1) {
            return false;
        }
        Set<Character> s = new HashSet<Character>();
        for (char c : mission.toCharArray())
            if (spies.indexOf(c) != -1)
                s.add(c);
        if (s.size() > 1) {
            return new Random().nextBoolean();
        }
        return true; //todo larger teams
    }

    @Override
    public void get_Traitors(int traitors) {
        for (Map.Entry<Boolean[], Double> entry : permutations.entrySet()) {
            Double score = oracleSabotages(mission, traitors, entry.getKey());
            entry.setValue(entry.getValue() + score);
        }
    }

    @Override
    public String do_Accuse() {
        return "";
    }

    @Override
    public void get_Accusation(String accuser, String accused) {

    }
}