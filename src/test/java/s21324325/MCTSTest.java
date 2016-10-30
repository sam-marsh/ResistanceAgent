package s21324325;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Author: Sam Marsh
 * Date: 29/10/2016
 */
public class MCTSTest {

    @Test
    public void test() {
        GameState state = new GameState("ABCDE", "AB", 'A');
        state.round(5);
        state.phase(GameState.Phase.NOMINATION);
        state.failures(2);
        state.nominationAttempt(1);
        state.traitors(0);
        state.currentLeader(0);

        MCTS mcts = new MCTS(state);
        mcts.search();
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //int[] scores= mcts.simulate(state);
        //System.out.println(Arrays.toString(scores));
        mcts.toString();
        System.out.println(mcts.transition());
    }

}