package search;

import core.Agent;

/**
 * The spy component of {@link SearchAgent}, using Monte Carlo Tree Search.
 */
public class SearchSpyAgent implements Agent {

    //we only have one second to make our move... This is the amount of time we sleep the game thread in
    // order to search for as long as possible.
    private static final int DELAY_TIME = 900;

    //whether the agent has been started yet.
    private boolean initialised;

    //the game state from the perspective of us (a spy - i.e. perfect information)
    private GameState state;

    //the searcher, used to pick our moves.
    private MCTS searcher;

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            state = new GameState(players, spies, name.charAt(0));
            searcher = new MCTS(state);
            initialised = true;
        }
        //update the state
        state.round(mission);
        state.failures(failures);
        state.nominationAttempt(1);
        state.traitors(0);
        state.phase(GameState.Phase.NOMINATION);

        //shut down the searcher so that the program can end (otherwise has random thread waiting so program doesn't
        // terminate)
        if (state.complete() && searcher != null) {
            searcher.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Nominate(int number) {
        //update game state
        state.phase(GameState.Phase.NOMINATION);
        state.currentLeader(state.players().indexOf(state.me()));

        //start the search
        searcher.state(state);
        searcher.search();

        //delay as long as possible
        sleep(DELAY_TIME);

        //get the best move
        MCTS.Transition transition = searcher.transition();

        //perform the move
        return ((ResistanceTransition.Nomination) transition).selection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_ProposedMission(String leader, String mission) {
        //update game state
        state.currentLeader(state.players().indexOf(leader));
        state.mission(mission);
        state.phase(GameState.Phase.VOTING);
        state.currentPlayer(state.players().indexOf(state.me()));

        state.nominationAttempt(state.nominationAttempt() + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean do_Vote() {
        searcher.state(state);
        searcher.search();

        //delay as long as possible
        sleep(DELAY_TIME);

        //get the best move
        MCTS.Transition transition = searcher.transition();

        //perform the move
        return ((ResistanceTransition.Vote) transition).yes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Votes(String yays) {
        //update state
        state.nominationAttempt(state.nominationAttempt() + 1);
        state.phase(GameState.Phase.MISSION);
    }

    private String lastMission;

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Mission(String mission) {
        state.nominationAttempt(1);
        lastMission = mission;
        //update state
        state.mission(mission);
        state.phase(GameState.Phase.MISSION);
        state.currentPlayer(state.players().indexOf(state.me()));
        state.traitors(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean do_Betray() {
        searcher.state(state);
        //start the search
        searcher.search();

        //delay as long as possible
        sleep(DELAY_TIME);

        //get the best move
        MCTS.Transition transition = searcher.transition();

        //perform the move
        return ((ResistanceTransition.Sabotage) transition).sabotage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Traitors(int traitors) {
        //update state
        state.traitors(traitors);
        state.update(lastMission, traitors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Accuse() {
        //don't bother
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Accusation(String accuser, String accused) {
        //ignore
    }

    /**
     * Sleeps the current thread, ignores exceptions from interrupts.
     *
     * @param ms the time to sleep in milliseconds
     */
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {}
    }

}
