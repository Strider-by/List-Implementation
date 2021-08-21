package com.github.strider_by.util;

import java.util.Objects;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import java.util.RandomAccess;

@SuppressWarnings("unchecked")
public class LimitedCapacityArrayList<E> {

    private static final int DEFAULT_INITIAL_CAPACITY = 10;
    private static final int DEFAULT_MAXIMUM_CAPACITY = Integer.MAX_VALUE;
    private static final double EXTRA_FREE_CELLS_AFTER_EXTENSION_COEFFICIENT = 0.2;
    private static final double STORAGE_SIZE_TO_MAX_CAPACITY_SIZE_BEFORE_FULL_EXTENSION_COEFFICIENT = 0.9;
    
    private Object[] storage;
    private int cursor = 0;
    private int maxCapacity;

    public LimitedCapacityArrayList() {
        this(DEFAULT_MAXIMUM_CAPACITY, DEFAULT_INITIAL_CAPACITY);
    }

    public LimitedCapacityArrayList(int maxCapacity) {
        this(maxCapacity, Integer.min(DEFAULT_INITIAL_CAPACITY, maxCapacity));
    }

    public LimitedCapacityArrayList(int maxCapacity, int initialCapacity) {
        if(maxCapacity < 0 || initialCapacity < 0 || initialCapacity > maxCapacity) {
            throw new IllegalArgumentException("Invalid parameters acquired");
        }

        this.storage = new Object[initialCapacity];
        this.maxCapacity = maxCapacity;
    }
    
    public int size() {
        return cursor;
    }

    public boolean isEmpty() {
        return cursor == 0;
    }

    public boolean contains(Object o) {
        for (int i = 0; i < cursor; i++) {
            Object current = storage[i];
            if (Objects.equals(current, o)) {
                return true;
            }
        }
        
        return false;
    }

    public Object[] toArray() {
        return Arrays.copyOf(storage, cursor);
    }

    public <T> T[] toArray(T[] a) {
        return (T[]) Arrays.copyOf(storage, cursor + 1, a.getClass());
    }

    public <T> T[] toArray(IntFunction<T[]> generator) {
        T[] newArray = generator.apply(cursor);
        System.arraycopy(storage, 0, newArray, 0, cursor);

        return newArray;
    }

    public boolean add(E e) {
        if (!ensureCapacity()) {
            return false;
        }

        this.storage[cursor++] = e;
        return true;
    }

    public boolean remove(Object o) {
        for (int i = 0; i < cursor; i++) {
            Object current = storage[i];
            if (Objects.equals(current, o)) {
                remove(i);
                return true;
            }
        }

        return false;
    }

    public boolean addAll(Collection<? extends E> c) {
        if (!ensureCapacity(c.size())) {
            return false;
        }

        for (E element : c) {
            this.storage[cursor++] = element;
        }
        return true;
    }

    public boolean removeAll(Collection<?> c) {
        boolean storageChanged = false;

        for (int i = cursor - 1; i >= 0; i--) {
            Object current = storage[i];
            if (c.contains(current)) {
                remove(i);
                storageChanged = true;
            }
        }

        return storageChanged;
    }

    public boolean retainAll(Collection<?> c) {
        boolean storageChanged = false;

        for (int i = cursor - 1; i >= 0; i--) {
            Object current = storage[i];
            if (!c.contains(current)) {
                remove(i);
                storageChanged = true;
            }
        }

        return storageChanged;
    }

    public void replaceAll(UnaryOperator<E> operator) {
        for (int i = 0; i < cursor; i++) {
            storage[i] = operator.apply((E) storage[i]);
        }
    }

    public void sort(Comparator<? super E> c) {
        for (int i = cursor - 1; i > 0; i--) {
            for (int j = 0; j < i; j++) {
                E current = (E) storage[j];
                E next = (E) storage[j+1];
                if (c.compare(current, next) > 0) {
                    // swapping items
                    storage[j] = next;
                    storage[j+1] = current;
                }
            }
        }
    }

    public void clear() {
        cursor = 0;
        Arrays.fill(storage, null);
    }

    public E get(int index) {
        checkIndex(index);
        return (E) storage[index];
    }

    public E set(int index, E element) {
        checkIndex(index);
        E old = (E) storage[index];
        storage[index] = element;

        return old;
    }

    public boolean add(int index, E element) {
        checkIndex(index);
        if (!ensureCapacity()) {
            return false;
        }

        for (int i = cursor++; i > index; i--) {
            storage[i] = storage[i-1];
        }
        storage[index] = element;

        return true;
    }

    public E remove(int index) {
        checkIndex(index);
        E item = (E) storage[index];

        for (int i = index; i < cursor; i++) {
            storage[i] = storage[i+1];
        }
        storage[--cursor] = null;

        return item;
    }

    public int indexOf(Object o) {
        for (int i = 0; i < cursor; i++) {
            Object current = storage[i];
            if (Objects.equals(current, o)) {
                return i;
            }
        }

        return -1;
    }

    public int lastIndexOf(Object o) {
        for (int i = cursor - 1; i >= 0; i--) {
            Object current = storage[i];
            if (Objects.equals(current, o)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * This is NOT a canonical sublist.
     * This method returns a new, separate List, not a "live" part if this particular ArrayList instance.
     */
    public List<E> subList(int fromIndexInclusive, int toIndexExclusive) {
        checkIndex(fromIndexInclusive);
        checkIndex(toIndexExclusive - 1);
        if (toIndexExclusive <= fromIndexInclusive) {
            throw new IllegalArgumentException("Provided indexes are not valid");
        }

        java.util.ArrayList<E> sublist = new java.util.ArrayList<>(toIndexExclusive - fromIndexInclusive);
        for (int i = fromIndexInclusive; i < toIndexExclusive; i++) {
            sublist.add((E) storage[i]);
        }

        return sublist;
    }

    @Override
    public String toString() {
        String content = Arrays.stream(storage)
                .limit(cursor)
                .map(Object::toString)
                .reduce((acc, item) -> acc + ", " + item)
                .orElse("");

        return String.format("[%s]", content);
    }

    ////////////////////
    // special class methods
    ////////////////////

    public boolean canBeAdded(int requiredExtraCells) {
        return maxCapacity - cursor - requiredExtraCells > 0;
    }

    public int getFreeCellsCount() {
        return maxCapacity - cursor;
    }

    public int getCurrentCapacity() {
        return storage.length;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * Check inner storage and expand it if necessary
     * @param requiredExtraCells - how many items you want to additionally place inside
     * @return if you can add that many items
     */
    public boolean ensureCapacity(int requiredExtraCells) {

        boolean ensured;

        if (storage.length - cursor - requiredExtraCells >= 0) {
            // no extension required
            ensured = true;
        } else if (storageCanBeExtended(requiredExtraCells)) {
            // storage needs to be and will be extended
            extendStorage(cursor + requiredExtraCells);
            ensured = true;
        } else {
            // extension required but can not be made
            ensured = false;
        }

        return ensured;
    }

    /**
     * Check inner storage and expand it if necessary (up to requested size, not a bit more)
     * @param requiredExtraCells - how many items you want to additionally place inside
     * @return if you can add that many items
     */
    public boolean provideExactExtraCapacity(int requiredExtraCells) {

        boolean extensionIsPossible = storageCanBeExtended(requiredExtraCells);
        if (extensionIsPossible) {
            if (extensionRequired(requiredExtraCells)) {
                setStorageToSize(cursor + requiredExtraCells);
            }
        }

        return extensionIsPossible;
    }

    public void trimToSize() {
        setStorageToSize(cursor);
    }

    /////////////////////
    // ancillary
    /////////////////////

    private void checkIndex(int index) {
        if (index < 0 || index >= cursor) {
            throw new IndexOutOfBoundsException("Provided index cannot be processed");
        }
    }

    private boolean ensureCapacity() {
        return ensureCapacity(1);
    }

    private boolean extensionRequired(int requiredExtraCells) {
        return storage.length - cursor - requiredExtraCells < 0;
    }

    private boolean storageCanBeExtended() {
        return storageCanBeExtended(1);
    }

    private boolean storageCanBeExtended(int requiredExtraCells) {
        return maxCapacity - cursor - requiredExtraCells >= 0;
    }

    private void extendStorage(int requiredCapacity) {

        if (requiredCapacity < 0 || requiredCapacity > maxCapacity) {
            throw new IllegalArgumentException("Invalid capacity requested");
        }

        int newStorageSize;

        long optimalCapacity = requiredCapacity + Math.round(requiredCapacity * EXTRA_FREE_CELLS_AFTER_EXTENSION_COEFFICIENT);
        boolean fullExtensionRequired =
                (double) optimalCapacity / maxCapacity >= STORAGE_SIZE_TO_MAX_CAPACITY_SIZE_BEFORE_FULL_EXTENSION_COEFFICIENT;

        // it works even for the case where our optimalCapacity exceeded maxCapacity
        // so there is no need to perform this check explicitly
        if (fullExtensionRequired) {
            newStorageSize = maxCapacity;
        } else {
            newStorageSize = (int) optimalCapacity;
        }

        storage = Arrays.copyOf(storage, newStorageSize);
    }

    private void setStorageToSize(int size) {

        if (size < 0 || size > maxCapacity) {
            throw new IllegalArgumentException("Invalid capacity requested");
        }

        storage = Arrays.copyOf(storage, size);
        cursor = Math.min(cursor, storage.length);
    }
    
}
