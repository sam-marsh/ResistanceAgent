package s21324325.mcts.impl;

import s21324325.mcts.Perspective;
import org.junit.Test;

/**
 * Author: Sam Marsh
 * Date: 21/10/2016
 */
public class PerspectiveTest {

    @Test
    public void test() {
        Perspective p = new Perspective('A', new char[] {'A', 'B', 'C', 'D', 'E'}, 2);
        p.update(new char[] { 'A', 'B', 'C' }, 1);
        System.out.println(p);
    }

}