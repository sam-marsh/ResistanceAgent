package s21329882;

import cits3001_2016s2.Agent;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is the spy agent. It uses Bayesian inference to decide on transitions based on which options will minimise the
 * incorrectness of the opponent's knowledge.
 */
public class BayesSpyAgent implements Agent {

    //if a mission is sabotaged by us, the suspicion of the spies will increase - this constant holds the maximum
    // suspicion increase ratio for any spy that may be caused by us sabotaging, before we decide to/not to sabotage.
    // At the moment, we will not sabotage if it causes the suspicion of any spy to double (or more)
    private static final double MAX_ACCEPTABLE_SUSPICION_INCREASE = 2.0;

    //whether the game has started and everything has been set up
    private boolean initialised;

    //holds the game info, updated with each get_ method
    private GameState state;

    //the view of the game from each resistance member's perspective - including who we think they think are spies
    private Set<ResistancePerspective> perspectives;

    //used to keep track of who the leader of the current mission is, since it isn't passed to the get method
    private String lastNominatedLeader;
    private String lastNominatedMission;

    //my identifier character
    private char me;

    //which members are spies
    private String spies;

    private ExecutorService service;

    /**
     * Creates a new spy agent.
     */
    public BayesSpyAgent() {
        lastNominatedLeader = null;
        lastNominatedMission = null;
        service = Executors.newCachedThreadPool();
        initialised = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            me = name.charAt(0);
            this.spies = spies;
            state = new GameState(players, spies);
            perspectives = new HashSet<ResistancePerspective>(players.length() - spies.length());
        }

        state.missionNumber(mission);
        state.failures(failures);

        if (mission == 6) {
            service.shutdownNow();
        }
    }

    private boolean contains(String s, char c) {
        return s.indexOf(c) != -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Nominate(int number) {
        if (!initialised) {
            //replace the spy string with question marks for the point of view of resistance members
            String resistanceString = spies.replace("[^?]", "?");
            for (Character c : state.players()) {
                if (spies.indexOf(c) == -1) {
                    perspectives.add(
                            new ResistancePerspective(state, String.valueOf(c), new String(state.players()), resistanceString)
                    );
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append(me);

            List<Character> resistance = new ArrayList<Character>();
            for (char c : state.players())
                if (!contains(spies, c))
                    resistance.add(c);

            Collections.shuffle(resistance);
            for (char c : state.players()) {
                if (sb.length() == number)
                    break;
                sb.append(c);
            }

            resistance = new ArrayList<Character>();
            for (char c : spies.toCharArray())
                resistance.add(c);
            Collections.shuffle(resistance);
            resistance.remove(new Character(me));

            for (char c : resistance) {
                if (sb.length() == number)
                    break;
                sb.append(c);
            }

            initialised = true;
            return sb.toString();
        }

        return computeUncertainty(
                number, 0, 0,  new boolean[state.numberOfPlayers()],
                new AbstractMap.SimpleEntry<String, Double>(null, Double.MAX_VALUE)
        ).getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_ProposedMission(String leader, String mission) {
        lastNominatedLeader = leader;
        lastNominatedMission = mission;
        state.proposedMission(new GameState.Mission(leader, mission));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean do_Vote() {
        //always accept my own missions
        if (state.proposedMission().leader() == me)
            return true;

        //TODO: improve this
        return numberOfSpiesOnMission(state.proposedMission()) >= 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Votes(String yays) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Mission(String mission) {
        //pass the leader if we know it, which should always be the case
        if (mission.equals(lastNominatedMission)) {
            state.mission(new GameState.Mission(lastNominatedLeader, lastNominatedMission));
        } else {
            state.mission(new GameState.Mission(null, mission));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean do_Betray() {
        //betray without calculation under some circumstances
        if (shouldSabotageByExpertRules()) {
            return true;
        }

        //now - simulate that every spy on the team will sabotage (worst case in terms of suspicion)
        int n = numberOfSpiesOnMission(state.mission());
        state.mission().done(1);

        //after the below, this will hold the average suspicion for each spy before
        Map<Character, Double> suspicion = new HashMap<Character, Double>(spies.length());
        for (Character c : spies.toCharArray())
            suspicion.put(c, 0.0);

        boolean continuing = true;

        //iterate over every resistance perspective
        for (ResistancePerspective perspective : perspectives) {

            //record previous suspicions for each player so we can 'rollback' after simulation
            Map<Player, Double> tmp = new HashMap<Player, Double>(perspective.players().size());
            for (Player player : perspective.players())
                tmp.put(player, player.bayesSuspicion());

            //update the suspicion based on the sabotages
            perspective.updateSuspicion();

            //sum up the suspicion increase of the spies
            for (Player p : perspective.players()) {
                if (spies.indexOf(p.id()) != -1) {
                    double newSuspicion = p.bayesSuspicion();
                    double oldSuspicion = tmp.get(p);

                    //don't sabotage if there is a risk that it will reveal a spy
                    if (newSuspicion == 1 && oldSuspicion < 1) {
                        continuing = false;
                        break;
                    }

                    suspicion.put(p.id(), suspicion.get(p.id()) + newSuspicion / oldSuspicion);
                }
            }

            //rollback suspicion values
            for (Map.Entry<Player, Double> entry : tmp.entrySet())
                entry.getKey().bayesSuspicion(entry.getValue());

            if (!continuing)
                break;
        }

        //rollback mission simulation
        state.mission().undo();

        if (!continuing)
            return false;

        //average the suspicions
        for (Character c : spies.toCharArray()) {
            suspicion.put(c, suspicion.get(c) / perspectives.size());
        }

        //check that no spy's suspicion is increasing too much
        for (Map.Entry<Character, Double> entry : suspicion.entrySet()) {
            if (entry.getValue() > MAX_ACCEPTABLE_SUSPICION_INCREASE) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Traitors(int traitors) {
        state.mission().done(traitors);

        //start updating all resistance perspectives in parallel
        for (final ResistancePerspective perspective : perspectives) {
            perspective.updateSuspicion();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Accuse() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Accusation(String accuser, String accused) {}


    /**
     * The expert rules for sabotage. Currently sabotages if
     *  - it will win the game
     *  - we are at risk of losing the game
     *  - it is the last round (redundant)
     *
     * @return true if we should sabotage
     */
    private boolean shouldSabotageByExpertRules() {
        return state.spyPoints() == 2 || state.resistancePoints() == 2 || state.round() == 5;
    }

    /**
     * Utility method to calculate the number of spies on a mission.
     *
     * @param mission the mission to check
     * @return the number of spies on the mission
     */
    private int numberOfSpiesOnMission(GameState.Mission mission) {
        int n = 0;
        for (char c : mission.team())
            if (spies.indexOf(c) != -1)
                ++n;
        return n;
    }

    /**
     * Calculates the 'incorrectness' of the average resistance member's guess as to who is the spy, given our opponent
     * model of the resistance. We try to maximise this incorrectness to win the game as a spy. This is used when
     * nominating a team to go on a mission. Every mission contains us - not particularly because it is an advantage,
     * but since it may be considered suspicious not to choose us on our team.
     *
     * @param select the number of players to be in the mission
     * @param start recursive parameter - pass 0
     * @param curr recursive parameter - pass 0
     * @param used recursive parameter - pass an array of false values of size {@link GameState#numberOfPlayers()}
     */
    private Map.Entry<String, Double> computeUncertainty(int select, int start, int curr, boolean[] used, final Map.Entry<String, Double> min) {
        //recursive base case
        if (curr == select) {
            //create the team string
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < used.length; ++i) {
                if (used[i]) {
                    sb.append(state.players()[i]);
                }
            }
            //ignore if we're not in the team
            if (sb.toString().indexOf(me) == -1)
                return min;

            //create a fake mission
            GameState.Mission fake = new GameState.Mission(String.valueOf(me), sb.toString());
            state.mission(fake);
            int sabotaged = numberOfSpiesOnMission(fake);
            state.mission().done(sabotaged);

            final AtomicDouble total = new AtomicDouble();
            final double unknown = (double) state.numberOfSpies() / (state.numberOfPlayers() - 1);

            Collection<Callable<Object>> collection = new ArrayList<Callable<Object>>();

            //update suspicion values of resistance members
            for (final ResistancePerspective perspective : perspectives) {
                collection.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        Map<Player, Double> tmp = new HashMap<Player, Double>(perspective.players().size());
                        for (Player player : perspective.players())
                            tmp.put(player, player.bayesSuspicion());

                        if (total.value > min.getValue()) return null;

                        perspective.updateSuspicion();

                        //sum up how wrong each player's suspicion of the spies is
                        for (Player player : perspective.players()) {
                            if (!player.equals(perspective.me())) {
                                total.increment(Math.pow(player.bayesSuspicion() - unknown, 2));
                                if (total.value > min.getValue()) break;
                            }
                        }

                        //reset suspicion
                        for (Map.Entry<Player, Double> entry : tmp.entrySet())
                            entry.getKey().bayesSuspicion(entry.getValue());

                        return null;
                    }
                });
            }

            try {
                Collection<Future<Object>> futures = service.invokeAll(collection);
                for (Future<Object> future : futures) future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (total.value < min.getValue()) {
                return new AbstractMap.SimpleEntry<String, Double>(sb.toString(), total.value);
            } else if (total.value == min.getValue()) {
                if (Math.random() < 0.5) {
                    return new AbstractMap.SimpleEntry<String, Double>(sb.toString(), total.value);
                }
            }

            return min;
        }
        //another base case - finished
        if (start == state.numberOfPlayers()) return min;

        //use the player at the start index in the team and recurse
        used[start] = true;
        Map.Entry<String, Double> newMin = computeUncertainty(select, start + 1, curr + 1, used, min);

        //don't use the player at the start index in the team and recurse
        used[start] = false;
        return computeUncertainty(select, start + 1, curr, used, newMin);
    }

    private class AtomicDouble {

        volatile double value = 0;

        synchronized void increment(double by) {
            value += by;
        }

    }

}
