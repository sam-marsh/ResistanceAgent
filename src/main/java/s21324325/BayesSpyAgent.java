package s21324325;

import cits3001_2016s2.Agent;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is the spy agent. It uses Bayesian inference to decide on transitions based on which options will minimise the
 * opponent's21329882 knowledge (i.e. maximises the uncertainty in our opponents).
 */
public class BayesSpyAgent implements Agent {

    //whether the game has started and everything has been set up
    private boolean initialised;

    //holds the game info, updated with each get_ method
    private GameState state;

    //the view of the game from each resistance member's21329882 perspective - including who we think they think are spies
    private Set<ResistancePerspective> perspectives;

    //my identifier character
    private char me;

    //which members are spies
    private String spies;

    //threading for updating resistance members bayesian probabilities in parallel - within game rules, since
    // only used while our do_() method is being called and not any other time
    private ExecutorService service;

    /**
     * Creates a new spy agent.
     */
    BayesSpyAgent() {
        service = Executors.newCachedThreadPool();
        initialised = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            //half-initialise - still need to construct resistance perspectives, but wait for do_ method
            me = name.charAt(0);
            this.spies = spies;
            state = new GameState(players, spies);
            perspectives = new HashSet<ResistancePerspective>(players.length() - spies.length());
        }

        state.round(mission);
        state.failures(failures);

        if (state.gameOver()) {
            service.shutdownNow();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Nominate(int number) {
        if (!initialised) {
            for (Character c : state.players()) {
                if (spies.indexOf(c) == -1) {
                    perspectives.add(
                            new ResistancePerspective(
                                    state, String.valueOf(c), new String(state.players())
                            )
                    );
                }
            }

            //first round - create random group
            StringBuilder sb = new StringBuilder();
            sb.append(me);

            //create list of resistance players
            List<Character> resistance = new ArrayList<Character>();
            for (char c : state.players())
                if (!BayesAgent.contains(spies, c))
                    resistance.add(c);

            //add resistance members until reaches enough players
            Collections.shuffle(resistance);
            for (char c : resistance) {
                if (sb.length() == number) {
                    break;
                }
                sb.append(c);
            }

            initialised = true;
            return sb.toString();
        }

        //pick the group which will induce the most uncertainty in resistance members if all
        // spies sabotage
        return maximumUncertaintyChoice(
                number, 0, 0,  new boolean[state.numberOfPlayers()],
                new AbstractMap.SimpleEntry<String, Double>(null, Double.MAX_VALUE)
        ).getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_ProposedMission(String leader, String mission) {
        state.proposedMission(new GameState.Mission(leader, mission));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean do_Vote() {
        if (!initialised) {
            for (Character c : state.players()) {
                if (spies.indexOf(c) == -1) {
                    perspectives.add(
                            new ResistancePerspective(
                                    state, String.valueOf(c), new String(state.players())
                            )
                    );
                }
            }
            initialised = true;
            return true;
        }

        //always accept my own missions
        if (state.proposedMission().leader() == me)
            return true;

        //accept any mission where the spies can win a point
        return numberOfSpiesOnMission(state.proposedMission()) >= numSabotagesRequiredForPoint();
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
        if (BayesAgent.same(mission, state.proposedMission().team())) {
            state.mission(state.proposedMission());
        } else {
            //mission must have been forcefully allocated
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

        //otherwise, check that we aren't at risk of identifying any spies with certainty (if so, don't sabotage)

        //now - simulate that every spy on the team will sabotage (worst case in terms of suspicion)
        int n = numberOfSpiesOnMission(state.mission());
        state.mission().done(n);

        boolean sabotaging = true;

        //iterate over every resistance perspective
        for (ResistancePerspective perspective : perspectives) {

            //record previous suspicions for each player so we can 'rollback' after simulation
            Map<ResistancePerspective.Player, Double> tmp = new HashMap<ResistancePerspective.Player, Double>(perspective.players().size());
            for (ResistancePerspective.Player player : perspective.players())
                tmp.put(player, player.bayesSuspicion());

            //update the suspicion based on the sabotages
            perspective.updateSuspicion();

            //sum up the suspicion increase of the spies
            for (ResistancePerspective.Player p : perspective.players()) {
                if (spies.indexOf(p.id()) != -1) {
                    double newSuspicion = p.bayesSuspicion();
                    double oldSuspicion = tmp.get(p);

                    //don't sabotage if there is a risk that it will reveal a spy
                    if (newSuspicion == 1 && oldSuspicion < 1) {
                        sabotaging = false;
                        break;
                    }
                }
            }

            //rollback suspicion values
            for (Map.Entry<ResistancePerspective.Player, Double> entry : tmp.entrySet())
                entry.getKey().bayesSuspicion(entry.getValue());

            if (!sabotaging)
                break;
        }

        //rollback mission simulation
        state.mission().undo();

        //still sabotage occasionally just for fun
        return sabotaging || Math.random() < 0.3;
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
        //all resistance players and their total spy probabilities summed across the resistance perspectives
        Map<Character, Double> resistances = new HashMap<Character, Double>();
        for (ResistancePerspective perspective : perspectives) {
            resistances.put(perspective.me().id(), 0.0);
        }
        //sum up total bayes suspicion
        for (ResistancePerspective perspective : perspectives) {
            for (ResistancePerspective.Player p : perspective.players()) {
                if (resistances.keySet().contains(p.id())) {
                    resistances.put(p.id(), resistances.get(p.id()) + p.bayesSuspicion());
                }
            }
        }
        //sort by suspicion descending
        List<Map.Entry<Character, Double>> list = new ArrayList<Map.Entry<Character, Double>>(resistances.entrySet());
        Collections.shuffle(list);
        Collections.sort(list, new Comparator<Map.Entry<Character, Double>>() {
            @Override
            public int compare(Map.Entry<Character, Double> o1, Map.Entry<Character, Double> o2) {
                return (int) Math.signum(o2.getValue() - o1.getValue());
            }
        });
        //add most suspicious non-spy players until reaches the number of spies in the game
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<Character, Double>> iterator = list.iterator();
        while (iterator.hasNext() && sb.length() < spies.length()) {
            sb.append(iterator.next().getKey());
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Accusation(String accuser, String accused) {
        //nope
    }


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
     * Calculates the 'uncertainty' of the average resistance member's21329882 guess as to who is the spy, given our opponent
     * model of the resistance. We try to maximise this uncertainty to win the game as a spy. This is used when
     * nominating a team to go on a mission. Every mission contains us - not particularly because it is an advantage,
     * but since it may be considered suspicious not to choose us on our team.
     *
     * @param select the number of players to be in the mission
     * @param start recursive parameter - pass 0
     * @param curr recursive parameter - pass 0
     * @param used recursive parameter - pass an array of false values of size {@link GameState#numberOfPlayers()}
     */
    private Map.Entry<String, Double> maximumUncertaintyChoice(int select, int start, int curr, boolean[] used, final Map.Entry<String, Double> min) {
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
            int sabotaged = numberOfSpiesOnMission(fake);

            //don't want to nominate anything which won't score us a point
            if (sabotaged != numSabotagesRequiredForPoint()) {
                return min;
            }

            //fake a mission
            state.mission(fake);
            state.mission().done(sabotaged);

            //used to hold the total uncertainty
            final AtomicDouble total = new AtomicDouble();

            //this is what we try to minimise - this probability is the initial probability in the resistance perspective,
            // we try to keep the probabilities close to this value
            final double unknown = (double) state.numberOfSpies() / (state.numberOfPlayers() - 1);

            //threading - execute all these in parallel to speed things up
            Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();

            //update suspicion values of resistance members
            for (final ResistancePerspective perspective : perspectives) {
                tasks.add(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        Map<ResistancePerspective.Player, Double> tmp = new HashMap<ResistancePerspective.Player, Double>(perspective.players().size());
                        for (ResistancePerspective.Player player : perspective.players())
                            tmp.put(player, player.bayesSuspicion());

                        //shortcut for efficiency
                        if (total.value > min.getValue()) return null;

                        perspective.updateSuspicion();

                        //sum up how uncertainty of players
                        for (ResistancePerspective.Player player : perspective.players()) {
                            if (!player.equals(perspective.me())) {
                                total.increment(Math.pow(player.bayesSuspicion() - unknown, 2));

                                //again, shortcut if we don't need to go any further
                                if (total.value > min.getValue()) break;
                            }
                        }

                        //reset suspicion
                        for (Map.Entry<ResistancePerspective.Player, Double> entry : tmp.entrySet())
                            entry.getKey().bayesSuspicion(entry.getValue());

                        return null;
                    }
                });
            }

            try {
                //run them all in parallel
                Collection<Future<Object>> futures = service.invokeAll(tasks);
                for (Future<Object> future : futures) future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (total.value < min.getValue()) {
                //return this one as a better option
                return new AbstractMap.SimpleEntry<String, Double>(sb.toString(), total.value);
            } else if (total.value == min.getValue()) {
                //equal values - return either one
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
        Map.Entry<String, Double> newMin = maximumUncertaintyChoice(select, start + 1, curr + 1, used, min);

        //don't use the player at the start index in the team and recurse
        used[start] = false;
        return maximumUncertaintyChoice(select, start + 1, curr, used, newMin);
    }

    /**
     * Want to always nominate and vote for teams with this many spies.
     *
     * @return the number of traitors required for us to get a point
     */
    private int numSabotagesRequiredForPoint() {
        return state.round() == 4 && state.numberOfPlayers() >= 7 ? 2 : 1;
    }

    /**
     * Mini-class for holding a double value that is accessed and incremented from multiple threads
     */
    private class AtomicDouble {

        //volatile so visible across threads quickly
        private volatile double value = 0;

        /**
         * Increments the value. Thread-safe.
         *
         * @param by the amount to increment by
         */
        private synchronized void increment(double by) {
            value += by;
        }

    }

}
