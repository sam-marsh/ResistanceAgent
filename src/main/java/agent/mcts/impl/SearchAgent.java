package agent.mcts.impl;

import agent.mcts.MCTS;
import agent.mcts.Transition;
import agent.mcts.impl.transition.SabotageTransition;
import agent.mcts.impl.transition.NominationTransition;
import agent.mcts.impl.transition.VoteTransition;
import core.Agent;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class SearchAgent implements Agent {

    private boolean initialised;
    private GameState state;
    private MCTS searcher;

    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (spies.contains("?")) System.exit(0);
        if (!initialised) {
            state = new GameState(players, spies, name.charAt(0));
            state.phase(GameState.Phase.NOMINATION);
            searcher = new MCTS(state);
            initialised = true;
        }
        state.round(mission);
        state.failures(failures);
        state.nominationAttempt(1);
        if (mission == 6 && searcher != null) {
            searcher.shutdown();
        }
    }

    @Override
    public String do_Nominate(int number) {
        long start = System.currentTimeMillis();
        state.phase(GameState.Phase.NOMINATION);
        state.currentPlayer(state.players().indexOf(state.me()));
        state.currentLeader(state.players().indexOf(state.me()));

        searcher.state(state);
        searcher.search();

        long diff = System.currentTimeMillis() - start;

        try {
            Thread.sleep((1000 - diff) * 4 / 5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Transition transition = searcher.transition();

        System.out.println("MAKING MOVE " + ((NominationTransition) transition).selection());
        return ((NominationTransition) transition).selection();
    }

    @Override
    public void get_ProposedMission(String leader, String mission) {
        state.currentLeader(state.players().indexOf(leader));
        state.mission(mission);
        state.phase(GameState.Phase.VOTING);
        state.currentPlayer(state.players().indexOf(state.me()));
        if (state.currentLeader() != state.players().indexOf(state.me())) {
            long start = System.currentTimeMillis();

            searcher.state(state);
            searcher.search();

            long diff = System.currentTimeMillis() - start;

            try {
                Thread.sleep((100 - diff) * 4 / 5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public boolean do_Vote() {
        if (state.currentLeader() == state.players().indexOf(state.me())) return true;

        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(state);
        long t1 = System.currentTimeMillis();
        Transition transition = searcher.transition();
        long t2 = System.currentTimeMillis();
        System.out.println("VOTING: " + (t2 - t1) + " " + ((VoteTransition) transition).yes());

        return ((VoteTransition) transition).yes();
    }

    @Override
    public void get_Votes(String yays) {
        state.nominationAttempt(state.nominationAttempt() + 1);
    }

    @Override
    public void get_Mission(String mission) {
        state.mission(mission);
        if (mission.indexOf(state.me()) != -1) {
            long start = System.currentTimeMillis();
            state.phase(GameState.Phase.MISSION);
            state.currentPlayer(state.players().indexOf(state.me()));

            searcher.state(state);
            searcher.search();
            long diff = System.currentTimeMillis() - start;
            try {
                Thread.sleep((100 - diff) * 3 / 4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean do_Betray() {
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Transition transition = searcher.transition();

        System.out.println("SABOTAGING: " + ((SabotageTransition) transition).sabotage());
        return ((SabotageTransition) transition).sabotage();
    }

    @Override
    public void get_Traitors(int traitors) {
        //unused
    }

    @Override
    public String do_Accuse() {
        //don't bother
        return "";
    }

    @Override
    public void get_Accusation(String accuser, String accused) {
        //ignore
    }

}
