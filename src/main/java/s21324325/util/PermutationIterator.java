package s21324325.util;

import java.util.*;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class PermutationIterator<T> implements Iterator<T[]> {

    private final int[] swaps;
    private T[] array;

    public PermutationIterator(Collection<T> collection) {
        this(collection, collection.size());
    }

    @SuppressWarnings("unchecked")
    public PermutationIterator(Collection<T> collection, int size) {
        this.array = (T[]) collection.toArray();
        this.swaps = new int[size];
        for (int i = 0; i < swaps.length; ++i) {
            swaps[i] = i;
        }
    }

    @Override
    public boolean hasNext() {
        return array != null;
    }

    @Override
    public T[] next() {
        if (array == null) throw new IllegalStateException();
        T[] res = Arrays.copyOf(array, swaps.length);
        int i = swaps.length - 1;
        while (i >= 0 && swaps[i] == array.length - 1) {
            swap(array, i, swaps[i]);
            swaps[i] = i;
            --i;
        }
        if (i < 0) {
            array = null;
        } else {
            int prev = swaps[i];
            swap(array, i, prev);
            int next = prev + 1;
            swaps[i] = next;
            swap(array, i, next);
        }
        return res;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void swap(T[] array, int i, int j) {
        T tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

}
