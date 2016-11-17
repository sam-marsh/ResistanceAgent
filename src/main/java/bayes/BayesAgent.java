package bayes;

import core.Agent;

import java.util.Collection;

/**
 * The Bayesian inference agent.
 */
public class BayesAgent implements Agent {

    /**
     * Whether the agent has been set up.
     */
    private boolean initialised;

    /**
     * This holds either a {@link BayesSpyAgent} or a {@link BayesResistanceAgent} depending
     * on which team we get allocated to. All callbacks are delegated to this object.
     */
    private Agent delegate;

    /**
     * Creates a new Bayesian agent.
     */
    public BayesAgent() {
        initialised = false;
        delegate = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            delegate = (spies.contains("?") ? new BayesResistanceAgent() : new BayesSpyAgent());
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

    /**
     * @param s a string
     * @param collection a collection of characters
     * @return true if the collection of characters and the string contain exactly the same characters
     */
    static boolean same(String s, Collection<Character> collection) {
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
    static boolean contains(String s, char c) {
        return s.indexOf(c) != -1;
    }

}
