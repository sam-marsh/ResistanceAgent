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
        GameState state = new GameState("D", "ABCDE", "??");
        state.lookup('A').bayesSuspicion(0.5);
        state.lookup('B').bayesSuspicion(0.3);
        state.lookup('C').bayesSuspicion(0.4);
        state.lookup('D').bayesSuspicion(0);
        state.lookup('E').bayesSuspicion(0.4);
        for (Player p : state.players()) {
            for (Player q : state.players()) {
                p.friendship(q);
            }
        }
        BayesSuspicionUpdater bayes = new BayesSuspicionUpdater(state);
        state.mission(new Mission(state, "A", "AB"));
        state.mission().done(1);
        bayes.missionOver();
        System.out.println(state.players());
        double total = 0;
        for (Player p : state.players()) total += p.bayesSuspicion();
        System.out.println(total);
    }
}