package agent.mcts;

import agent.bayes.BayesAgent;
import core.Agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Author: Sam Marsh
 * Date: 14/10/2016
 */
public class MCTSAgent implements Agent {

    private boolean initialised = false;
    private State state;
    private DecisionAgent me;
    private boolean amSpy;

    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            amSpy = !spies.contains("?");
            state = new State();
            state.players = new HashMap<Character, SimulationAgent>(players.length());
            for (char c : players.toCharArray()) {
                if (name.indexOf(c) == -1) {
                    state.players.put(c, new RandomAgent());
                } else {
                    state.players.put(c, me = new DecisionAgent());
                }
            }
            state.spies = new HashSet<Character>();
            Iterator<Character> iterator = state.players.keySet().iterator();
            while (state.spies.size() < spies.length()) {
                char c = iterator.next();
                if (c != name.charAt(0))
                    state.spies.add(c);
            }
            state.playerString = players;
            state.spyString = "";
            for (Character c : state.spies) state.spyString += c;
            state.resString = state.spyString;
            state.numPlayers = players.length();
            state.rand = new Random();
            state.rand.setSeed(state.rand.nextLong());
            state.fails = failures;
            state.round = 0;
            state.voteRound = 0;
            state.leader = 0;
            state.team = null;
            state.phase = State.Phase.NOT_STARTED;
            initialised = true;
        }

        state.round++;
        state.voteRound = 0;
        state.leader = state.rand.nextInt(state.numPlayers);
        state.phase = State.Phase.NOMINATING;
    }

    @Override
    public String do_Nominate(int number) {
        return "";
    }

    @Override
    public void get_ProposedMission(String leader, String mission) {
        state.voteRound++;
        state.phase = State.Phase.VOTING;
        state.leader = state.playerString.indexOf(leader);
        state.team = mission;
    }

    @Override
    public boolean do_Vote() {
        int successTrue = 0;
        int successFalse = 0;
        for (int i = 0; i < 10000; ++i) {
            Simulator simulator = new Simulator(state);
            me.votePolicy = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return true;
                }
            };
            if (simulator.play() == !amSpy)
                successTrue++;
        }
        for (int i = 0; i < 10000; ++i) {
            Simulator simulator = new Simulator(state);
            me.votePolicy = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return false;
                }
            };
            if (simulator.play() == !amSpy)
                successFalse++;
        }
        System.out.println("Choosing to vote " + (successTrue >= successFalse ? "yes." : "no."));
        System.out.println("Certainty: " + successTrue + " to " + successFalse);
        return successTrue >= successFalse;
    }

    @Override
    public void get_Votes(String yays) {

    }

    @Override
    public void get_Mission(String mission) {
        state.phase = State.Phase.MISSION;
        state.team = mission;
    }

    @Override
    public boolean do_Betray() {
        int successTrue = 0;
        int successFalse = 0;
        for (int i = 0; i < 10000; ++i) {
            Simulator simulator = new Simulator(state);
            me.betrayPolicy = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return true;
                }
            };
            if (simulator.play() == !amSpy)
                successTrue++;
        }
        for (int i = 0; i < 10000; ++i) {
            Simulator simulator = new Simulator(state);
            me.betrayPolicy = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return false;
                }
            };
            if (simulator.play() == !amSpy)
                successFalse++;
        }
        System.out.println(successTrue - successFalse);
        System.out.println("Choosing " + (successTrue >= successFalse ? "" : "not") + " to sabotage.");
        System.out.println("Certainty: " + successTrue + " to " + successFalse);
        return successTrue >= successFalse;
    }

    @Override
    public void get_Traitors(int traitors) {
        if(traitors !=0 && (traitors !=1 || state.round !=4 || state.numPlayers<7)){
            state.fails++;
        }
    }

    @Override
    public String do_Accuse() {
        return "";
    }

    @Override
    public void get_Accusation(String accuser, String accused) {

    }

    private class DecisionAgent implements SimulationAgent {

        Callable<Boolean> votePolicy;
        Callable<String> nominatePolicy;
        Callable<Boolean> betrayPolicy;

        @Override
        public SimulationAgent copy() {
            return this;
        }

        @Override
        public void get_status(String name, String players, String spies, int mission, int failures) {

        }

        @Override
        public String do_Nominate(int number) {
            try {
                return nominatePolicy.call();
            } catch (Exception e) {
                return "";
            }
        }

        @Override
        public void get_ProposedMission(String leader, String mission) {

        }

        @Override
        public boolean do_Vote() {
            try {
                return votePolicy.call();
            } catch (Exception e) {
                return new Random().nextBoolean();
            }
        }

        @Override
        public void get_Votes(String yays) {

        }

        @Override
        public void get_Mission(String mission) {

        }

        @Override
        public boolean do_Betray() {
            try {
                return betrayPolicy.call();
            } catch (Exception e) {
                return new Random().nextBoolean();
            }
        }

        @Override
        public void get_Traitors(int traitors) {

        }

        @Override
        public String do_Accuse() {
            return "";
        }

        @Override
        public void get_Accusation(String accuser, String accused) {

        }
    }

}
