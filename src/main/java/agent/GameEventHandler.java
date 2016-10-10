package agent;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: Sam Marsh
 * Date: 10/10/2016
 */
public class GameEventHandler implements GameEventListener {

    private Set<GameEventListener> listeners;

    public GameEventHandler() {
        listeners = new HashSet<GameEventListener>();
    }

    public void register(GameEventListener listener) {
        listeners.add(listener);
    }

    public void deregister(GameEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void status(String name, String players, String spies, int mission, int failures) {
        for (GameEventListener gel : listeners)
            gel.status(name, players, spies, mission, failures);
    }

    @Override
    public void proposedMission(String leader, String mission) {
        for (GameEventListener gel : listeners)
            gel.proposedMission(leader, mission);
    }

    @Override
    public void votes(String yays) {
        for (GameEventListener gel : listeners)
            gel.votes(yays);
    }

    @Override
    public void mission(String mission) {
        for (GameEventListener gel : listeners)
            gel.mission(mission);
    }

    @Override
    public void traitors(int traitors) {
        for (GameEventListener gel : listeners)
            gel.traitors(traitors);
    }

    @Override
    public void accusation(String accuser, String accused) {
        for (GameEventListener gel : listeners)
            gel.accusation(accuser, accused);
    }

}
