package s21324325.util;

import java.util.*;

/**
 * Author: Sam Marsh
 * Date: 18/10/2016
 */
public class CombinationIterator<T> implements Iterator<Collection<T>> {

    private final T[] array;
    private final int[] indices;
    private long combinations;
    private long left;

    @SuppressWarnings("unchecked")
    public CombinationIterator(Collection<T> collection, int size) {
        if (size > collection.size()) {
            throw new IllegalArgumentException("combination size greater than # elements in collection");
        }
        this.array = (T[]) collection.toArray();

        long sf = factorial(collection.size());
        long lf = factorial(size);
        long df = factorial(collection.size() - size);
        this.combinations = sf / (lf * df);
        this.left = this.combinations;

        this.indices = new int[size];
        init();
    }

    private void init() {
        for (int i = 0; i < indices.length; ++i) {
            indices[i] = i;
        }
        left = combinations;
    }

    @Override
    public boolean hasNext() {
        return left > 0;
    }

    @Override
    public Collection<T> next() {
        if (left == 0) {
            throw new NoSuchElementException();
        } else if (left < combinations) {
            int i = indices.length - 1;
            while (indices[i] == array.length - indices.length + i) {
                --i;
            }
            ++indices[i];
            for (int j = i + 1; j < indices.length; ++j) {
                indices[j] = indices[i] + j - i;
            }
        }
        --left;

        Collection<T> collection = new HashSet<T>();
        for (int i : indices) {
            collection.add(array[i]);
        }
        return collection;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private long factorial(long k) {
        long total = 1;
        while (k > 1) {
            total *= k;
            --k;
        }
        return total;
    }

}
