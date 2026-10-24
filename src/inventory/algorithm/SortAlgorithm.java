package inventory.algorithm;

import inventory.model.Product;

/**
 * ALGORITHM 3 - Insertion Sort, written by hand (no {@code Arrays.sort}).
 *
 * <p>Works on the {@code Product[]} produced by
 * {@link inventory.structures.ProductLinkedList#toArray()}. The array is sorted in
 * place; the linked list itself is never reordered, the sorted array is simply
 * what the table shows.</p>
 *
 * <p>Insertion sort is used because it is stable, easy to explain in the defense
 * and fast for a small inventory: O(n) when the data is already sorted,
 * O(n^2) worst case.</p>
 */
public class SortAlgorithm {

    public static final String BY_ID = "ID";
    public static final String BY_NAME = "Name";
    public static final String BY_QUANTITY = "Quantity";
    public static final String BY_PRICE = "Price";

    /** The fields the GUI offers in the "Sort by" combo box. */
    public static final String[] FIELDS = {BY_ID, BY_NAME, BY_QUANTITY, BY_PRICE};

    /**
     * Sorts the array in place.
     *
     * @param data      products to sort
     * @param field     one of {@link #BY_ID}, {@link #BY_NAME}, {@link #BY_QUANTITY}, {@link #BY_PRICE}
     * @param ascending true for A-Z / 0-9, false for the reverse
     */
    public static void sort(Product[] data, String field, boolean ascending) {
        if (data == null || data.length < 2) {
            return;
        }
        for (int i = 1; i < data.length; i++) {
            Product current = data[i];
            int j = i - 1;
            while (j >= 0 && compare(data[j], current, field, ascending) > 0) {
                data[j + 1] = data[j];
                j--;
            }
            data[j + 1] = current;
        }
    }

    /**
     * Compares two products on the chosen field.
     *
     * @return a negative number when {@code left} must come first, a positive
     *         number when {@code right} must come first, 0 when they are equal
     */
    private static int compare(Product left, Product right, String field, boolean ascending) {
        int result;
        if (BY_ID.equals(field)) {
            result = left.getId().compareToIgnoreCase(right.getId());
        } else if (BY_NAME.equals(field)) {
            result = left.getName().compareToIgnoreCase(right.getName());
        } else if (BY_QUANTITY.equals(field)) {
            result = Integer.compare(left.getQuantity(), right.getQuantity());
        } else {
            result = Double.compare(left.getPrice(), right.getPrice());
        }
        if (ascending) {
            return result;
        }
        return -result;
    }
}
