package agent.bayes;

import agent.GameState;
import agent.Mission;
import agent.Player;
import agent.ResistancePerspective;
import core.Agent;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This is the spy agent. It uses Bayesian inference to decide on moves based on which options will minimise the
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

    //for parallel computation of spy probabilities
    private ExecutorService executor;
    private Set<Future<?>> futures;

    //my identifier character
    private char me;

    //which members are spies
    private String spies;

    /**
     * Creates a new spy agent.
     */
    public BayesSpyAgent() {
        lastNominatedLeader = null;
        lastNominatedMission = null;
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

            //replace the spy string with question marks for the point of view of resistance members
            String resistanceString = spies.replace("[^?]", "?");
            for (Character c : players.toCharArray()) {
                if (spies.indexOf(c) == -1) {
                    perspectives.add(
                            new ResistancePerspective(state, String.valueOf(c), players, resistanceString)
                    );
                }
            }

            //one thread for each resistance member
            executor = Executors.newFixedThreadPool(perspectives.size());
            futures = new HashSet<Future<?>>();
            initialised = true;
        }

        state.missionNumber(mission);
        state.failures(failures);

        if (state.gameOver()) {
            waitForCalculation();
            //force shutdown executor - shouldn't be necessary but just in case
            executor.shutdownNow();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Nominate(int number) {
        //wait for executor to finish computation
        waitForCalculation();

        //find incorrectness for each resistance member's prediction based on each team containing me, sort by maximum
        // incorrectness, and return the one that makes the resistance members the most wrong in their inference
        Map<String, Double> map = new HashMap<String, Double>();
        computeUncertainty(number, 0, 0, new boolean[state.numberOfPlayers()], map);
        List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(map.entrySet());
        Collections.shuffle(list);
        //sort descending
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return (int) Math.signum(o1.getValue() - o2.getValue());
            }
        });

        //return the first value
        return list.get(0).getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_ProposedMission(String leader, String mission) {
        lastNominatedLeader = leader;
        lastNominatedMission = mission;
        state.proposedMission(new Mission(state, leader, mission));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean do_Vote() {
        waitForCalculation();

        //always accept my own missions
        if (state.proposedMission().leader() == me)
            return true;

        //TODO: improve this
        return numberOfSpiesOnMission(state.proposedMission()) >= 1;
    }

    @Override
    public void get_Votes(String yays) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Mission(String mission) {
        //pass the leader if we know it, which should always be the case
        if (mission.equals(lastNominatedMission)) {
            state.mission(new Mission(state, lastNominatedLeader, lastNominatedMission));
        } else {
            state.mission(new Mission(state, null, mission));
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

        waitForCalculation();

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

    @Override
    public void get_Traitors(int traitors) {
        state.mission().done(traitors);

        for (final ResistancePerspective perspective : perspectives) {
            Future<?> future = executor.submit(new Runnable() {
                @Override
                public void run() {
                    perspective.updateSuspicion();
                }
            });
            futures.add(future);
        }
    }

    @Override
    public String do_Accuse() {
        return "";
    }

    @Override
    public void get_Accusation(String accuser, String accused) {}

    /**
     * Utility method to wait for all executing calculations (generally of suspicion values using Bayes' rule) to
     * finish.
     */
    private void waitForCalculation() {
        Iterator<Future<?>> iterator = futures.iterator();
        while (iterator.hasNext()) {
            Future<?> future = iterator.next();
            try {
                future.get();
            } catch (Exception ignore) {}
            iterator.remove();
        }
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
    private int numberOfSpiesOnMission(Mission mission) {
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
     * @param map the map to place the computed values in. The keys are the possible teams to choose, along with their
     *            incorrectness values.
     */
    private void computeUncertainty(int select, int start, int curr, boolean[] used, Map<String, Double> map) {
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
                return;

            //create a fake mission
            Mission fake = new Mission(state, String.valueOf(me), sb.toString());
            state.mission(fake);
            int sabotaged = numberOfSpiesOnMission(fake);
            state.mission().done(sabotaged);

            double total = 0;
            double unknown = state.numberOfSpies() / (state.numberOfPlayers() - 1);

            //update suspicion values of resistance members
            for (ResistancePerspective perspective : perspectives) {
                Map<Player, Double> tmp = new HashMap<Player, Double>(perspective.players().size());
                for (Player player : perspective.players())
                    tmp.put(player, player.bayesSuspicion());

                perspective.updateSuspicion();

                //sum up how wrong each player's suspicion of the spies is
                double wrongness = 0;
                for (Player player : perspective.players()) {
                    if (!player.equals(perspective.me())) {
                        wrongness += Math.pow(player.bayesSuspicion() - unknown, 2);
                    }
                }

                //add to the total
                total += wrongness;

                //reset suspicion
                for (Map.Entry<Player, Double> entry : tmp.entrySet())
                    entry.getKey().bayesSuspicion(entry.getValue());
            }

            map.put(sb.toString(), total);
            return;
        }
        //another base case - finished
        if (start == state.numberOfPlayers()) return;

        //use the player at the start index in the team and recurse
        used[start] = true;
        computeUncertainty(select, start + 1, curr + 1, used, map);

        //don't use the player at the start index in the team and recurse
        used[start] = false;
        computeUncertainty(select, start + 1, curr, used, map);
    }

    /**
     * Method for debugging/testing.
     * @return the game state
     */
    public GameState state() {
        return state;
    }

}
