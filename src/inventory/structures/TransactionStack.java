package inventory.structures;

import inventory.model.Transaction;

/**
 * DATA STRUCTURE 3 - Stack (array based, LIFO).
 *
 * <p>Holds the transaction history, newest row on top. Undo Last Action walks
 * down this stack with {@link #pop()}: rows that are already undone (or that are
 * {@code UNDO} rows) are skipped and pushed back in their original order, so the
 * log is never damaged. The action that is reversed is put back on the stack
 * with an extra {@code UNDO} row on top of it.</p>
 *
 * <p>The array doubles its capacity when it is full, so push is amortized O(1).</p>
 */
public class TransactionStack {

    private static final int INITIAL_CAPACITY = 8;

    private Transaction[] items = new Transaction[INITIAL_CAPACITY];
    private int size;

    /** Adds a transaction on top of the stack. */
    public void push(Transaction transaction) {
        if (transaction == null) {
            return;
        }
        if (size == items.length) {
            grow();
        }
        items[size++] = transaction;
    }

    /** Removes and returns the top transaction, or null when the stack is empty. */
    public Transaction pop() {
        if (size == 0) {
            return null;
        }
        int last = --size;
        Transaction transaction = items[last];
        items[last] = null;
        return transaction;
    }

    /** Returns the top transaction without removing it, or null when empty. */
    public Transaction peek() {
        if (size == 0) {
            return null;
        }
        return items[size - 1];
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    /**
     * Copies the stack into a plain array, newest first (top to bottom),
     * which is exactly the order the history table shows.
     */
    public Transaction[] toArray() {
        Transaction[] result = new Transaction[size];
        for (int i = 0; i < size; i++) {
            result[i] = items[size - 1 - i];
        }
        return result;
    }

    /** Doubles the array when no slot is left. */
    private void grow() {
        Transaction[] bigger = new Transaction[items.length * 2];
        for (int i = 0; i < size; i++) {
            bigger[i] = items[i];
        }
        items = bigger;
    }
}
