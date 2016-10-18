package agent.util;

import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import static org.junit.Assert.*;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class CombinationIteratorTest {

    @Test
    public void testIterator() {
        Collection<Character> collection = new LinkedList<Character>();
        collection.add('A');
        collection.add('B');
        collection.add('C');
        collection.add('D');
        collection.add('E');
        Iterator<Collection<Character>> iterator = new CombinationIterator<Character>(collection, 1);
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
    }

}