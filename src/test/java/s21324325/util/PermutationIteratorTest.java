package s21324325.util;

import org.junit.Test;

import java.util.*;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class PermutationIteratorTest {

    @Test
    public void permutationTest() {
        Collection<Character> collection = new LinkedList<Character>();
        collection.add('A');
        collection.add('B');
        collection.add('C');
        collection.add('D');
        collection.add('E');
        Iterator<Character[]> iterator = new PermutationIterator<Character>(collection, 3);
        int i = 0;
        while (iterator.hasNext()) {
            ++i;
            System.out.println(Arrays.toString(iterator.next()));
        }
        System.out.println(i);
    }
}