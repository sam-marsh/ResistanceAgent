package search;

import core.Agent;
import bayes.BayesResistanceAgent;

/**
 * The Monte Carlo Search agent.
 */
public class SearchAgent implements Agent {

    //whether the agent has been set up.
    private boolean initialised;

    //all get_ and do_ methods are just passed to this
    private Agent delegate;

    /**
     * Creates a new Search agent.
     */
    public SearchAgent() {
        initialised = false;
        delegate = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            delegate = (spies.contains("?") ? new BayesResistanceAgent() : new SearchSpyAgent());
            initialised = true;
        }
        delegate.get_status(name, players, spies, mission, failures);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Nominate(int number) {
        return delegate.do_Nominate(number);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_ProposedMission(String leader, String mission) {
        delegate.get_ProposedMission(leader, mission);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean do_Vote() {
        return delegate.do_Vote();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Votes(String yays) {
        delegate.get_Votes(yays);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Mission(String mission) {
        delegate.get_Mission(mission);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean do_Betray() {
        return delegate.do_Betray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Traitors(int traitors) {
        delegate.get_Traitors(traitors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String do_Accuse() {
        return delegate.do_Accuse();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_Accusation(String accuser, String accused) {
        delegate.get_Accusation(accuser, accused);
    }

}
