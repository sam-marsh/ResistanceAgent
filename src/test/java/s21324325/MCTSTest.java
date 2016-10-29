package s21324325;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Author: Sam Marsh
 * Date: 29/10/2016
 */
public class MCTSTest {

    @Test
    public void test() {
        GameState state = new GameState("ABCDE", "AB", 'A');
        state.round(1);
        state.phase(GameState.Phase.NOMINATION);
        state.failures(0);
        state.nominationAttempt(1);
        state.traitors(0);
        state.currentLeader(0);

        MCTS mcts = new MCTS(state);
        mcts.search();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mcts.toString();
        System.out.println(mcts.transition());
    }

}