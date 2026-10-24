package inventory.structures;

import inventory.model.Product;

/**
 * DATA STRUCTURE 2 - Hash table with separate chaining.
 *
 * <p>Indexes products by their ID so the service can find, check, update and
 * delete a product in close to O(1) instead of scanning the whole list.
 * The hash function is written by hand (a polynomial over the characters);
 * {@code hashCode()} is not used.</p>
 *
 * <p>The table has a fixed capacity - it is not resized - so a bucket may hold a
 * small chain of entries. That collision chain is the "separate chaining" part.</p>
 */
public class ProductHashTable {

    /** Number of buckets. 101 is prime, which spreads IDs well. */
    private static final int CAPACITY = 101;

    /** One node inside a bucket chain. */
    private static class Entry {
        private String key;
        private Product value;
        private Entry next;

        Entry(String key, Product value) {
            this.key = key;
            this.value = value;
        }
    }

    private final Entry[] buckets = new Entry[CAPACITY];
    private int size;

    /**
     * Polynomial hash written by hand:
     * {@code h = (h * 31 + character) % CAPACITY} for every character of the key.
     * The modulo is applied at every step, so the value can never overflow.
     */
    private int hash(String key) {
        int hashValue = 0;
        for (int i = 0; i < key.length(); i++) {
            hashValue = (hashValue * 31 + key.charAt(i)) % CAPACITY;
        }
        return hashValue;
    }

    /** Adds a product, or replaces the product already stored under the same ID. */
    public void put(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        int index = hash(product.getId());
        Entry current = buckets[index];
        while (current != null) {
            if (current.key.equals(product.getId())) {
                current.value = product;
                return;
            }
            current = current.next;
        }
        Entry entry = new Entry(product.getId(), product);
        entry.next = buckets[index];
        buckets[index] = entry;
        size++;
    }

    /** Fast exact lookup by ID. Returns null when the ID is not indexed. */
    public Product get(String id) {
        if (id == null) {
            return null;
        }
        Entry current = buckets[hash(id)];
        while (current != null) {
            if (current.key.equals(id)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    /** Removes the entry with the given ID. Returns the removed product or null. */
    public Product remove(String id) {
        if (id == null) {
            return null;
        }
        int index = hash(id);
        Entry previous = null;
        Entry current = buckets[index];
        while (current != null) {
            if (current.key.equals(id)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return current.value;
            }
            previous = current;
            current = current.next;
        }
        return null;
    }

    /** Used for the duplicate-ID check before a product is added. */
    public boolean containsKey(String id) {
        return get(id) != null;
    }

    public void clear() {
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = null;
        }
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Capacity of the bucket array (useful for the documentation/demo). */
    public int capacity() {
        return CAPACITY;
    }
}
