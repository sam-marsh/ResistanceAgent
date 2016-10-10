package agent;

import core.Agent;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public class SMAgent implements Agent {

    private GameEventHandler handler;
    private Agent delegate;
    private BayesSuspicionTracker tracker;
    private BayesSuspicionTracker resistanceTracker;
    private FriendshipTracker friendshipTracker;
    private boolean initialised;

    public SMAgent() {
        initialised = false;
    }

    @Override
    public void get_status(String name, String players, String spies, int mission, int failures) {
        if (!initialised) {
            delegate = new ResistanceSMAgent();

            handler = new GameEventHandler();

            tracker = new BayesSuspicionTracker(players, spies.length(), "");
            handler.register(tracker);

            if (spies.contains("?")) {
                resistanceTracker = new BayesSuspicionTracker(players, spies.length(), name);
                handler.register(resistanceTracker);
            }

            friendshipTracker = new FriendshipTracker(players);
            handler.register(friendshipTracker);

            initialised = true;
        }
        handler.status(name, players, spies, mission, failures);

        if (resistanceTracker != null) {
            for (char c : players.toCharArray()) {
                System.out.println(c + ": " + resistanceTracker.spyness(c));
            }
        }
    }

    @Override
    public String do_Nominate(int number) {
        return delegate.do_Nominate(number);
    }

    @Override
    public void get_ProposedMission(String leader, String mission) {
        delegate.get_ProposedMission(leader, mission);
        handler.proposedMission(leader, mission);
    }

    @Override
    public boolean do_Vote() {
        return delegate.do_Vote();
    }

    @Override
    public void get_Votes(String yays) {
        delegate.get_Votes(yays);
        handler.votes(yays);
    }

    @Override
    public void get_Mission(String mission) {
        delegate.get_Mission(mission);
        handler.mission(mission);
    }

    @Override
    public boolean do_Betray() {
        return delegate.do_Betray();
    }

    @Override
    public void get_Traitors(int traitors) {
        delegate.get_Traitors(traitors);
        handler.traitors(traitors);
    }

    @Override
    public String do_Accuse() {
        return delegate.do_Accuse();
    }

    @Override
    public void get_Accusation(String accuser, String accused) {
        delegate.get_Accusation(accuser, accused);
        handler.accusation(accuser, accused);
    }

}
