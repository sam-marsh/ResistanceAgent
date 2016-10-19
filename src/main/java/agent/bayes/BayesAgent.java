package agent.bayes;

import core.Agent;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public class BayesAgent implements Agent {

    private boolean initialised;
    private Agent delegate;

    public BayesAgent() {
        initialised = false;
        delegate = null;
    }

    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            delegate = (spies.contains("?") ? new BayesResistanceAgent() : new BayesSpyAgent());
            initialised = true;
        }
        delegate.get_status(name, players, spies, mission, failures);
    }

    @Override
    public String do_Nominate(int number) {
        return delegate.do_Nominate(number);
    }

    @Override
    public void get_ProposedMission(String leader, String mission) {
        delegate.get_ProposedMission(leader, mission);
    }

    @Override
    public boolean do_Vote() {
        return delegate.do_Vote();
    }

    @Override
    public void get_Votes(String yays) {
        delegate.get_Votes(yays);
    }

    @Override
    public void get_Mission(String mission) {
        delegate.get_Mission(mission);
    }

    @Override
    public boolean do_Betray() {
        return delegate.do_Betray();
    }

    @Override
    public void get_Traitors(int traitors) {
        delegate.get_Traitors(traitors);
    }

    @Override
    public String do_Accuse() {
        return delegate.do_Accuse();
    }

    @Override
    public void get_Accusation(String accuser, String accused) {
        delegate.get_Accusation(accuser, accused);
    }

}
