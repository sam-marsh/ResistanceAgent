package s21329882;

import cits3001_2016s2.Agent;

import java.util.*;

/**
 * This is the resistance player component of the Bayesian inference agent.
 *
 * It maintains a spy probability for each player, which is updated using Bayes' rule after
 * each mission, using a basic opponent model and the evidence gained from the number of sabotages
 * that occurred.
 *
 * In addition, it also exploits some expert rules (like always accepting the first team nominated in the game)
 * and uses some 'suspicion tracking' techniques based on the 2012 Vienna competition bot 'PandSBot' by Pavel Raliuk
 * and Alex Paklonski (https://github.com/aigamedev/resistance/blob/master/bots/1/pands.py). Specifically, the agent
 * considers the 'connection' between potential spy initialiseSpyCombinations (i.e. how often they vote for each other's missions),
 * etc. to influence the overall spy probability.
 *
 * In addition, suspicious and good actions are also tracked (like voting for a mission which failed = bad, or a player
 * who voted no for a nominated team which they were on = good).
 */
public class BayesResistanceAgent implements Agent {

    private static final double SUSPICION_CUTOFF = 0.99;

    private static final double FRIENDSHIP_WEIGHT = 0.5;

    //used to track whether this is the first time that get_status has been called
    private boolean initialised;

    //every combination of players which could make up the spies - maximum (9 choose 4) = 126
    private Map<Collection<Player>, Double> spyCombinations;

    //holds general game state information, like the current mission etc.
    private GameState state;

    //our perspective of the game - in particular, spy suspicions are computed here
    private ResistancePerspective perspective;

    //used for a bit of randomness in selecting teams
    private Random random;

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            //initialise variables but wait until do_() method to construct spy initialiseSpyCombinations
            state = new GameState(players, spies);
            perspective = new ResistancePerspective(state, name, players, spies);
            spyCombinations = new HashMap<Collection<Player>, Double>();
            random = new Random();
        }
        state.missionNumber(mission);
        state.failures(failures);

        if (initialised) {
            //update due to new suspicion values
            updateCombinationsSpyness();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Nominate(int number) {
        if (!initialised) {
            //first time - so set up spy initialiseSpyCombinations
            initialiseSpyCombinations();
            initialised = true;

            //select me + a random team first, since no information
            StringBuilder sb = new StringBuilder();
            sb.append(perspective.me().id());
            Collections.shuffle(perspective.others());
            for (Player p : perspective.others()) {
                if (sb.length() == number) break;
                sb.append(p.id());
            }
            return sb.toString();
        }

        //get the nice players
        Collection<Player> good = notInMostLikelySpyCombination();

        //add me + the lowest suspicion players
        StringBuilder sb = new StringBuilder();
        sb.append(perspective.me().id());
        for (Player p : good) if (sb.length() < number) sb.append(p.id());

        //pretty confident this will always hold since number to select is never
        // greater than the number of true resistance agents
        assert sb.length() == number;

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_ProposedMission(String leader, String mission) {
        state.proposedMission(new GameState.Mission(leader, mission));

        //suspicious if a leader doesn't put him/herself on their own team
        perspective.lookup(leader.charAt(0)).behavedLikeSpy().sample(!mission.contains(leader));
        updateCombinationsSpyness();
    }

    @Override
    public boolean do_Vote() {
        if (!initialised) {
            //first time - so set up spy initialiseSpyCombinations
            initialiseSpyCombinations();
            initialised = true;
            //always vote yes on the first team - no point even considering a choice
            return true;
        }

        GameState.Mission mission = state.proposedMission();

        //always vote for my own missions...
        if (mission.leader() == perspective.me().id()) {
            return true;
        }

        //always vote no on large teams that don't contain me, since these MUST contain a spy
        if (mission.team().size() >= (state.numberOfPlayers() - state.numberOfSpies())
                && !mission.team().contains(perspective.me().id())) {
            return false;
        }

        //otherwise, check that none of the players on the bad combination are in the team
        Collection<Player> bad = mostLikelySpyCombination();
        Collection<Character> coll = new ArrayList<Character>();
        for (Player p : bad)
            coll.add(p.id());

        return !intersects(coll, mission.team());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Votes(String yays) {
        state.proposedMission().voted(yays);

        GameState.Mission proposed = state.proposedMission();
        Set<Character> in = new HashSet<Character>(proposed.team());

        Set<Character> out = new HashSet<Character>(state.numberOfPlayers());
        for (Character p : proposed.team()) {
            out.remove(p);
        }

        Set<Character> votedYes = new HashSet<Character>(yays.length());
        for (char c : yays.toCharArray()) {
            votedYes.add(c);
        }

        for (Character c : state.players()) {
            Player p = perspective.lookup(c);
            //ignore if me
            if (c.equals(perspective.me().id())) continue;

            if (c.equals(proposed.leader())) {
                //friendship indicated for every player on the team nominated by the leader
                for (Character other : out) {
                    perspective.lookup(other).friendship(p, 1, out.size());
                }
            } else {
                if (!in.contains(c)) {
                    if (votedYes.contains(c)) {
                        //player voted for team but not on team - friendly to people on team
                        for (Character other : proposed.team()) {
                            p.friendship(perspective.lookup(other), 1, proposed.team().size());
                        }
                    } else {
                        //player voted against team and not on team - friendly to people not on team
                        for (Character other : out) {
                            p.friendship(perspective.lookup(other), 1, out.size());
                        }
                    }
                }
            }
        }

        for (Player p : perspective.players()) {
            //player voted against team when on team - quite resistance-like
            p.behavedLikeResistance().sample(
                    state.proposedMission().team().contains(p.id()) && !contains(yays, p.id())
            );
        }

        //adjust scores based on new information
        updateCombinationsSpyness();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Mission(String mission) {
        if (same(mission, state.proposedMission().team())) {
            state.mission(state.proposedMission());
        } else {
            //mission has been allocated forcefully since too many attempts
            state.mission(new GameState.Mission(null, mission));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean do_Betray() {
        throw new AssertionError("i thought i was a resistance agent");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Traitors(int traitors) {
        state.mission().done(traitors);

        if (state.mission().yays() != null) {
            for (Player player : perspective.players()) {
                if (!player.equals(perspective.me())) {
                    //voted for mission which failed
                    boolean hmm1 = (contains(state.mission().yays(), player.id()) && traitors > 0);
                    //voted against mission which succeeded
                    boolean hmm2 = (!contains(state.mission().yays(), player.id()) && traitors == 0);
                    player.helpedSpy().sample(hmm1 || hmm2 ? 1 : 0);
                }
            }
        }

        //update suspicion values based on number of traitors, update group suspicion values as well
        perspective.updateSuspicion();
        updateCombinationsSpyness();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Accuse() {
        //accuse those we know are spies with certainty
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
        //nope
    }

    /**
     * @return the spy combination which has displayed the most spyness
     */
    private Collection<Player> mostLikelySpyCombination() {
        Map.Entry<Collection<Player>, Double> max = null;

        //find the maximum entry, with a little bit of randomness thrown in
        for (Map.Entry<Collection<Player>, Double> entry : spyCombinations.entrySet()) {
            if (max == null || entry.getValue() * randBetween(0.95, 1.0) > max.getValue()) {
                max = entry;
            }
        }

        if (max == null) {
            throw new IllegalStateException("no possible spy combinations - uh oh");
        }

        return max.getKey();
    }

    /**
     * @return all players not in {@link #mostLikelySpyCombination()}
     */
    private Collection<Player> notInMostLikelySpyCombination() {
        List<Player> players = new ArrayList<Player>(perspective.others());
        Collection<Player> bad = mostLikelySpyCombination();

        //remove each player that is in the most suspicious group
        double value = spyCombinations.get(bad);
        if (value > 0) {
            for (Player p : bad) {
                players.remove(p);
            }
        }

        //sort by spyness in ascending order
        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player o1, Player o2) {
                return (int) Math.signum(o1.spyness() - o2.spyness());
            }
        });

        return players;
    }

    /**
     * Adds all possible combinations of spies to the {@link #spyCombinations} collection.
     */
    private void initialiseSpyCombinations() {
        List<Player> others = perspective.others();
        initialiseSpyCombinations(state.numberOfSpies(), 0, 0, others, new boolean[others.size()], spyCombinations);
        updateCombinationsSpyness();
    }

    /**
     * Helper method for {@link #initialiseSpyCombinations()}
     *
     * @param spies the number of spies in the game
     * @param start 0
     * @param curr 0
     * @param others a list of all players but not including me
     * @param used an array of false values of the same size as the others list
     * @param map an empty map
     */
    private void initialiseSpyCombinations(
            int spies, int start, int curr, List<Player> others,
            boolean[] used, Map<Collection<Player>, Double> map) {
        //base case - add this collection
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
        initialiseSpyCombinations(spies, start + 1, curr + 1, others, used, map);

        //don't use the player at the start index in the team and recurse
        used[start] = false;
        initialiseSpyCombinations(spies, start + 1, curr, others, used, map);
    }

    /**
     * Updates the suspicion value for each possible combination of spies.
     */
    private void updateCombinationsSpyness() {
        //need to use iterator object to avoid concurrent modification exception
        Iterator<Map.Entry<Collection<Player>, Double>> iterator = spyCombinations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Collection<Player>, Double> entry = iterator.next();

            //calculate overall spyness of group
            double estimate = 1;
            for (Player player : entry.getKey()) {
                estimate *= player.spyness();
            }

            //if any of the players are certainly resistance (will happen if bayesian inference engine
            // infers it) then get rid of this combination since it doesn't need to be considered any more
            if (estimate == 0) {
                iterator.remove();
                continue;
            }

            //don't change suspicion if we exceeds a certain value
            if (estimate < SUSPICION_CUTOFF) {
                double v = estimate;

                //weight according to how correlated the friendships are between players in the group
                double u = 1.0;
                for (Player player : entry.getKey()) {
                    for (Player other : entry.getKey()) {
                        if (!player.equals(other)) {
                            u *= player.friendship(other);
                        }
                    }
                }
                v *= ((1 - FRIENDSHIP_WEIGHT) + FRIENDSHIP_WEIGHT * u);

                //weight according to how much members of the group have been helpful to spies
                u = 1.0;
                for (Player player : entry.getKey()) {
                    u *= player.helpedSpy().value();
                }
                v *= ((1.0 - Player.HELPED_SPY_WEIGHT) + Player.HELPED_SPY_WEIGHT * u);

                //weight according to how much members of the group have behaved like spies
                u = 1.0;
                for (Player player : entry.getKey()) {
                    u *= player.behavedLikeSpy().value();
                }
                v *= ((1.0 - Player.BEHAVED_LIKE_SPY_WEIGHT) + Player.BEHAVED_LIKE_SPY_WEIGHT * u);

                //weight according to how much members of the group have behaved like resistance
                u = 1.0;
                for (Player player : entry.getKey()) {
                    u *= player.behavedLikeResistance().value();
                }
                v *= (1.0 - Player.BEHAVED_LIKE_RESISTANCE_WEIGHT * u);

                //update the value
                entry.setValue(v);
            }
        }
    }

    /**
     * @param coll1 a collection
     * @param coll2 a collection
     * @return true if the two collections have any elements in common, otherwise false
     */
    private <T> boolean intersects(Collection<T> coll1, Collection<T> coll2) {
        for (T t : coll1)
            if (coll2.contains(t))
                return true;
        return false;
    }

    /**
     * @param x an arbitrary real number
     * @param y an arbitrary real number greater than x
     * @return a random number (evenly distributed) between x and y
     */
    private double randBetween(double x, double y) {
        return random.nextDouble() * (y - x) + x;
    }

    /**
     * @param s a string
     * @param collection a collection of characters
     * @return true if the collection of characters and the string contain exactly the same characters
     */
    private boolean same(String s, Collection<Character> collection) {
        for (char c : collection) {
            if (!contains(s, c)) return false;
        }
        return true;
    }

    /**
     * @param s a string
     * @param c a character
     * @return true if the string contains the character
     */
    private boolean contains(String s, char c) {
        return s.indexOf(c) != -1;
    }

}
