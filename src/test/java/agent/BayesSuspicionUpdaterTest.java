package agent;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Author: Sam Marsh
 * Date: 13/10/2016
 */
public class BayesSuspicionUpdaterTest {

    @Test
    public void testShouldIdentifySpies() {
        GameState state = new GameState("D", "ABCDEF", "??");
        BayesSuspicionUpdater bayes = new BayesSuspicionUpdater(state);
        state.mission(new Mission(state, "A", "DE"));
        state.mission().done(1);
        bayes.missionOver();
        state.mission(new Mission(state, "A", "DEB"));
        state.mission().done(1);
        bayes.missionOver();
        state.mission(new Mission(state, "A", "DEAC"));
        state.mission().done(0);
        bayes.missionOver();
        state.mission(new Mission(state, "A", "DEB"));
        state.mission().done(1);
        bayes.missionOver();
        state.mission(new Mission(state, "A", "DEAB"));
        state.mission().done(2);
        bayes.missionOver();

        System.out.println(state.players());
    }
}