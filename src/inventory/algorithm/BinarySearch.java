package inventory.algorithm;

import inventory.model.Product;

/**
 * ALGORITHM 2 - Binary Search, written by hand.
 *
 * <p>Used by the Search box for <b>Product ID</b>. Binary search only works on an
 * array that is already sorted, so the service first copies the products with
 * {@code toArray()}, sorts them ascending by ID with
 * {@link SortAlgorithm#sort(Product[], String, boolean)} and then calls this class.
 * The cost is O(log n) after the sort.</p>
 */
public class BinarySearch {

    /**
     * Searches an ID-sorted array.
     *
     * @param sortedById products sorted ascending by ID
     * @param id         the exact ID to find
     * @return the index of the product, or -1 when it is not in the array
     */
    public static int indexOfById(Product[] sortedById, String id) {
        if (sortedById == null || id == null || sortedById.length == 0) {
            return -1;
        }
        int low = 0;
        int high = sortedById.length - 1;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            int comparison = sortedById[middle].getId().compareToIgnoreCase(id);
            if (comparison == 0) {
                return middle;
            }
            if (comparison < 0) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return -1;
    }

    /**
     * @return the product with that exact ID, or null when it is not found
     */
    public static Product findById(Product[] sortedById, String id) {
        int index = indexOfById(sortedById, id);
        if (index < 0) {
            return null;
        }
        return sortedById[index];
    }
}
